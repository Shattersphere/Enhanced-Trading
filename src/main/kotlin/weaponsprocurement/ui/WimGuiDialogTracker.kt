package weaponsprocurement.ui

/**
 * Tracks the single active custom dialog and the request-to-reopen handoff used after UI rebuilds.
 * It is deliberately small because Starsector owns the actual dialog lifecycle.
 */
class WimGuiDialogTracker<C, S> {
    private var open = false
    private var closeRequested = false
    private var pendingContext: C? = null
    private var pendingState: S? = null

    fun isOpen(): Boolean = open

    fun markOpen() {
        open = true
        closeRequested = false
    }

    fun markClosed() {
        open = false
        closeRequested = false
    }

    fun requestClose() {
        if (open) {
            closeRequested = true
        }
    }

    fun consumeCloseRequest(): Boolean {
        val result = closeRequested
        closeRequested = false
        return result
    }

    fun requestReopen(context: C, state: S) {
        pendingContext = context
        pendingState = state
        open = false
        closeRequested = false
    }

    fun hasPending(): Boolean = !open && pendingState != null

    fun consumePending(): WimGuiPendingDialog<C?, S?>? {
        if (!hasPending()) return null
        val pending = WimGuiPendingDialog(pendingContext, pendingState)
        pendingContext = null
        pendingState = null
        return pending
    }
}
