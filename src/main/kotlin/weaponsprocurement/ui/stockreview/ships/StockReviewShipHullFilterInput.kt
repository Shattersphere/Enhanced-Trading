package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import org.lwjgl.input.Keyboard
import weaponsprocurement.ui.WimGuiControls
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState

object StockReviewShipHullFilterInput {
    private const val LABEL_WIDTH = 100f
    private const val FIELD_WIDTH = 360f
    private const val GAP = 6f

    @JvmStatic
    fun render(root: CustomPanelAPI, state: StockReviewState) {
        val bounds = bounds()
        WimGuiControls.addLabel(
            root,
            "Hull Class:",
            StockReviewStyle.TEXT,
            bounds.labelLeft,
            bounds.top,
            LABEL_WIDTH,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            Alignment.RMID,
        )
        val field = root.createCustomPanel(
            FIELD_WIDTH,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            WimGuiPanelPlugin(StockReviewStyle.ACTION_BACKGROUND, StockReviewStyle.ROW_BORDER),
        )
        root.addComponent(field).inTL(bounds.fieldLeft, bounds.top)
        val label = state.getShipHullFilter().ifBlank { "" }
        WimGuiControls.addLabel(
            field,
            label,
            StockReviewStyle.TEXT,
            0f,
            0f,
            FIELD_WIDTH,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            Alignment.MID,
        )
    }

    @JvmStatic
    fun process(
        events: List<InputEventAPI>,
        root: CustomPanelAPI?,
        state: StockReviewState,
        focused: Boolean,
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
                val inField = bounds.contains(root, event)
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
                    val before = state.getShipHullFilter()
                    state.backspaceShipHullFilter()
                    changed = changed || before != state.getShipHullFilter()
                    event.consume()
                }
                Keyboard.KEY_DELETE -> {
                    val before = state.getShipHullFilter()
                    state.setShipHullFilter("")
                    changed = changed || before != state.getShipHullFilter()
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
                        val before = state.getShipHullFilter()
                        state.appendShipHullFilter(char)
                        changed = changed || before != state.getShipHullFilter()
                        event.consume()
                    }
                }
            }
        }
        return Result(nextFocused, changed)
    }

    data class Result(
        val focused: Boolean,
        val changed: Boolean,
    )

    private fun bounds(): Bounds {
        val top = StockReviewStyle.MODAL.actionRowY(0f, 0f)
        val fieldLeft = StockReviewStyle.MODAL.width - StockReviewStyle.PAD - FIELD_WIDTH
        return Bounds(fieldLeft - GAP - LABEL_WIDTH, fieldLeft, top)
    }

    private data class Bounds(
        val labelLeft: Float,
        val fieldLeft: Float,
        val top: Float,
    ) {
        fun contains(root: CustomPanelAPI, event: InputEventAPI): Boolean {
            val screenLeft = root.position.x + fieldLeft
            val screenRight = screenLeft + FIELD_WIDTH
            val screenTop = root.position.y + root.position.height - top
            val screenBottom = screenTop - StockReviewStyle.ACTION_BUTTON_HEIGHT
            return event.x >= screenLeft &&
                event.x <= screenRight &&
                event.y >= screenBottom &&
                event.y <= screenTop
        }
    }
}
