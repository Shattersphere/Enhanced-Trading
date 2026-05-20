package weaponsprocurement.ui

import com.fs.starfarer.api.ui.TextFieldAPI

/**
 * Singleton registry that tracks every currently-rendered text field, polls them each
 * frame for focus transitions, and dispatches commit callbacks on blur. Only one modal
 * is open at a time, so a shared registry is safe.
 *
 * Lifecycle:
 *  - On modal `init()`: caller invokes [resetAll] to wipe persistent focus memory.
 *  - At the start of every `rebuildContent()` pass: caller invokes [clearBindings] to
 *    drop dead TextFieldAPI references; new bindings are registered as the row renderer
 *    walks the row tree.
 *  - Every `advance()` tick: caller invokes [pollFrame] to honour focus-gain / focus-loss
 *    side effects and dispatch commit callbacks.
 */
object WimGuiTextFieldRegistry {
    private data class Binding(
        val key: String,
        val field: TextFieldAPI,
        val blurText: String,
        val onCommit: ((Int) -> Unit)?,
        val onCommitString: ((String) -> Unit)?,
        val live: Boolean,
        var lastCommittedDisplay: String,
        var lastSeenText: String,
        var lastFocus: Boolean,
    )

    private val bindings = LinkedHashMap<String, Binding>()
    private val focusMemory = HashMap<String, Boolean>()
    private var focusedKey: String? = null
    private var committedThisFrame: Boolean = false

    /** True once when any text field committed since the last [consumeCommitDirty] call. */
    @JvmStatic
    fun consumeCommitDirty(): Boolean {
        val v = committedThisFrame
        committedThisFrame = false
        return v
    }

    /** Wipe everything; called when a modal is first opened. */
    @JvmStatic
    fun resetAll() {
        bindings.clear()
        focusMemory.clear()
        focusedKey = null
        committedThisFrame = false
    }

    /** Drop the current per-rebuild bindings (preserve persistent focus memory). */
    @JvmStatic
    fun clearBindings() {
        bindings.clear()
    }

    /**
     * Register a freshly-created [TextFieldAPI] under [key]. Restores focus if the same
     * [key] held focus before the rebuild, and resyncs the "blur placeholder" rendering
     * so a -1 ("no rule") committed value shows as [blurText].
     */
    @JvmStatic
    fun bind(
        key: String,
        field: TextFieldAPI,
        initialText: String,
        blurText: String,
        onCommit: (Int) -> Unit,
    ) {
        bindInternal(key, field, initialText, blurText, onCommit, null, live = false)
    }

    /** String-commit variant: passes the raw trimmed text instead of parsing as Int. */
    @JvmStatic
    fun bindString(
        key: String,
        field: TextFieldAPI,
        initialText: String,
        blurText: String,
        onCommit: (String) -> Unit,
    ) {
        bindInternal(key, field, initialText, blurText, null, onCommit, live = false)
    }

    /**
     * Live string-commit variant: fires [onCommit] on every text change while focused
     * (in addition to the usual blur commit). Used for incremental filter inputs where
     * the list should rebuild as the user types instead of waiting for blur.
     */
    @JvmStatic
    fun bindStringLive(
        key: String,
        field: TextFieldAPI,
        initialText: String,
        blurText: String,
        onCommit: (String) -> Unit,
    ) {
        bindInternal(key, field, initialText, blurText, null, onCommit, live = true)
    }

    private fun bindInternal(
        key: String,
        field: TextFieldAPI,
        initialText: String,
        blurText: String,
        onCommitInt: ((Int) -> Unit)?,
        onCommitString: ((String) -> Unit)?,
        live: Boolean,
    ) {
        val binding = Binding(
            key = key,
            field = field,
            blurText = blurText,
            onCommit = onCommitInt,
            onCommitString = onCommitString,
            live = live,
            lastCommittedDisplay = initialText,
            lastSeenText = initialText,
            lastFocus = false,
        )
        bindings[key] = binding
        field.setText(initialText)
        if (focusedKey == key) {
            field.grabFocus()
            if (field.getText() == blurText) field.setText("")
            binding.lastFocus = true
            binding.lastSeenText = field.getText() ?: ""
        }
    }

    /** Per-frame: detect focus transitions, dispatch commits, manage placeholder display. */
    @JvmStatic
    fun pollFrame() {
        if (bindings.isEmpty()) return
        var newFocusedKey: String? = null
        for (binding in bindings.values) {
            val field = binding.field
            val hasFocus = try {
                field.hasFocus()
            } catch (t: Throwable) {
                false
            }
            if (hasFocus) newFocusedKey = binding.key

            if (hasFocus && !binding.lastFocus) {
                // Focus gained: strip the blur placeholder so the user can type cleanly.
                if (field.getText() == binding.blurText) field.setText("")
                binding.lastSeenText = field.getText() ?: ""
            } else if (!hasFocus && binding.lastFocus) {
                // Focus lost: parse, commit, normalise display.
                commit(binding)
            } else if (hasFocus && binding.live && binding.onCommitString != null) {
                // Mid-typing: fire incremental string commit on any text change.
                val current = field.getText() ?: ""
                if (current != binding.lastSeenText) {
                    binding.lastSeenText = current
                    val raw = current.trim()
                    try {
                        binding.onCommitString.invoke(raw)
                        committedThisFrame = true
                    } catch (t: Throwable) {
                        // Swallow: never let a callback exception crash the polling loop.
                    }
                }
            }
            binding.lastFocus = hasFocus
            focusMemory[binding.key] = hasFocus
        }
        focusedKey = newFocusedKey
    }

    private fun commit(binding: Binding) {
        val raw = binding.field.getText()?.trim().orEmpty()
        if (binding.onCommitString != null) {
            try {
                binding.onCommitString.invoke(raw)
            } catch (t: Throwable) {
                binding.field.setText(binding.lastCommittedDisplay)
                return
            }
            val display = if (raw.isEmpty()) binding.blurText else raw
            binding.lastCommittedDisplay = display
            binding.field.setText(display)
            binding.lastSeenText = display
            committedThisFrame = true
            return
        }
        val onCommitInt = binding.onCommit ?: return
        val value: Int = when {
            raw.isEmpty() -> -1
            raw == binding.blurText -> -1
            raw.all { it.isDigit() } && raw.length <= 9 -> raw.toInt()
            else -> {
                // Invalid input: restore the last committed display, don't fire onCommit.
                binding.field.setText(binding.lastCommittedDisplay)
                return
            }
        }
        try {
            onCommitInt(value)
        } catch (t: Throwable) {
            // Defensive: never let a commit failure crash the polling loop. Restore display.
            binding.field.setText(binding.lastCommittedDisplay)
            return
        }
        val display = if (value < 0) binding.blurText else value.toString()
        binding.lastCommittedDisplay = display
        binding.field.setText(display)
        binding.lastSeenText = display
        committedThisFrame = true
    }
}
