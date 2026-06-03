package weaponsprocurement.ui.stockreview.rendering

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.shattersphere.shatterlib.gui.input.SearchWithClearPresentation
import com.shattersphere.shatterlib.starsector.ui.StarsectorSearchWithClear
import com.shattersphere.shatterlib.starsector.ui.StarsectorSearchWithClearStyle
import org.lwjgl.input.Keyboard
import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState

object StockReviewSearchInputSupport {
    private const val FIELD_WIDTH = 360f
    private val STYLE: StarsectorSearchWithClearStyle
        get() = StarsectorSearchWithClearStyle(
            fillColor = StockReviewStyle.ACTION_BACKGROUND,
            borderColor = StockReviewStyle.ROW_BORDER,
            focusedBorderColor = StockReviewStyle.TEXT,
            textColor = StockReviewStyle.TEXT,
            clearFillColor = StockReviewStyle.CANCEL_BUTTON,
            clearBorderColor = StockReviewStyle.ROW_BORDER,
            clearTextColor = StockReviewStyle.TEXT,
            disabledClearFillColor = StockReviewStyle.DISABLED_DARK,
            disabledClearTextColor = StockReviewStyle.DISABLED_TEXT,
        )

    @JvmStatic
    fun renderItemSearch(
        root: CustomPanelAPI,
        state: StockReviewState,
        focused: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        render(root, state.getItemSearch(), focused, StockReviewAction.clearItemSearch(), buttons)
    }

    @JvmStatic
    fun renderShipHullSearch(
        root: CustomPanelAPI,
        state: StockReviewState,
        focused: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        render(root, state.getShipHullFilter(), focused, StockReviewAction.clearShipHullFilter(), buttons)
    }

    @JvmStatic
    fun processItemSearch(
        events: List<InputEventAPI>,
        root: CustomPanelAPI?,
        state: StockReviewState,
        focused: Boolean,
    ): Result = process(events, root, focused, state::getItemSearch, state::appendItemSearch, state::backspaceItemSearch, state::setItemSearch)

    @JvmStatic
    fun processShipHullSearch(
        events: List<InputEventAPI>,
        root: CustomPanelAPI?,
        state: StockReviewState,
        focused: Boolean,
    ): Result = process(events, root, focused, state::getShipHullFilter, state::appendShipHullFilter, state::backspaceShipHullFilter, state::setShipHullFilter)

    private fun render(
        root: CustomPanelAPI,
        query: String,
        focused: Boolean,
        clearAction: StockReviewAction,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        val bounds = bounds()
        val result = StarsectorSearchWithClear.render(
            parent = root,
            x = bounds.left,
            y = bounds.top,
            width = FIELD_WIDTH,
            height = StockReviewStyle.ACTION_BUTTON_HEIGHT,
            presentation = SearchWithClearPresentation(
                query = query,
                focused = focused,
                placeholder = "type to filter",
                searchPrefix = "Search: ",
                searchTooltip = "Type to filter the visible stock rows.",
            ),
            style = STYLE,
            clearEnabled = query.isNotBlank(),
            clearButtonWidth = StockReviewStyle.ACTION_BUTTON_HEIGHT,
        )
        if (query.isNotBlank()) {
            buttons.add(WimGuiButtonBinding(result.clearPanel, null, clearAction))
        }
    }

    private fun process(
        events: List<InputEventAPI>,
        root: CustomPanelAPI?,
        focused: Boolean,
        query: () -> String,
        append: (Char) -> Unit,
        backspace: () -> Unit,
        set: (String?) -> Unit,
    ): Result {
        if (root == null || root.position == null) {
            return Result(focused, false)
        }
        var nextFocused = focused
        var changed = false
        val bounds = bounds()
        for (event in events) {
            if (event.isConsumed) {
                continue
            }
            if (event.isLMBDownEvent) {
                val inField = bounds.containsSearchField(root, event)
                if (nextFocused != inField) {
                    nextFocused = inField
                    changed = true
                }
                if (inField) {
                    event.consume()
                }
                continue
            }
            if (!nextFocused || !event.isKeyDownEvent || event.isModifierKey || event.eventValue == Keyboard.KEY_ESCAPE) {
                continue
            }
            when (event.eventValue) {
                Keyboard.KEY_BACK -> {
                    val before = query()
                    backspace()
                    changed = changed || before != query()
                    event.consume()
                }
                Keyboard.KEY_DELETE -> {
                    val before = query()
                    set("")
                    changed = changed || before != query()
                    event.consume()
                }
                Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                    nextFocused = false
                    changed = true
                    event.consume()
                }
                else -> {
                    val char = event.eventChar
                    if (!event.isCtrlDown && char.code >= 32) {
                        val before = query()
                        append(char)
                        changed = changed || before != query()
                        event.consume()
                    }
                }
            }
        }
        return Result(nextFocused, changed)
    }

    private fun bounds(): Bounds {
        val top = StockReviewStyle.MODAL.actionRowY(0f, 0f)
        val left = StockReviewStyle.MODAL.width - StockReviewStyle.PAD - FIELD_WIDTH
        return Bounds(left, top)
    }

    data class Result(
        val focused: Boolean,
        val changed: Boolean,
    )

    private data class Bounds(
        val left: Float,
        val top: Float,
    ) {
        fun containsSearchField(root: CustomPanelAPI, event: InputEventAPI): Boolean {
            val screenLeft = root.position.x + left
            val screenRight = screenLeft + FIELD_WIDTH - StockReviewStyle.ACTION_BUTTON_HEIGHT
            val screenTop = root.position.y + root.position.height - top
            val screenBottom = screenTop - StockReviewStyle.ACTION_BUTTON_HEIGHT
            return event.x >= screenLeft &&
                event.x <= screenRight &&
                event.y >= screenBottom &&
                event.y <= screenTop
        }
    }
}
