package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import org.lwjgl.input.Keyboard
import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiControls
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewShipFilterField
import weaponsprocurement.ui.stockreview.state.StockReviewShipSizeFilter
import weaponsprocurement.ui.stockreview.state.StockReviewState

/**
 * Ship-specific filter controls. Text input is intentionally custom because Starsector's
 * public campaign UI does not expose a reusable modal text-box widget for this surface.
 */
object StockReviewShipFilterModal {
    private const val FIELD_WIDTH = 176f
    private const val FIELD_HEIGHT = 22f
    private const val LABEL_WIDTH = 204f
    private const val LEFT_X = 18f
    private const val RIGHT_X = 454f
    private const val SIZE_TOP = 16f
    private const val FIELD_TOP = 54f
    private const val ROW_GAP = 6f
    private const val ROW_HEIGHT = 28f
    private const val BUTTON_GAP = 6f

    @JvmStatic
    fun render(
        panel: CustomPanelAPI,
        state: StockReviewState,
        focusedField: StockReviewShipFilterField?,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        var x = LEFT_X
        val buttonWidth = 149f
        for (size in StockReviewShipSizeFilter.values()) {
            val active = state.isShipSizeFilterActive(size)
            WimGuiControls.addBoundButton(
                panel,
                x,
                SIZE_TOP,
                FIELD_HEIGHT,
                WimGuiButtonSpec.toggle(
                    buttonWidth,
                    size.label,
                    StockReviewStyle.TEXT,
                    StockReviewAction.toggleShipSizeFilter(size),
                    Alignment.MID,
                    if (active) StockReviewStyle.FILTER_ACTIVE else StockReviewStyle.ACTION_BACKGROUND,
                    StockReviewStyle.ROW_BORDER,
                    if (active) "Remove this ship-size filter." else "Only show ${size.label.lowercase()} hulls.",
                    null,
                ),
                buttons,
            )
            x += buttonWidth + BUTTON_GAP
        }

        for (field in StockReviewShipFilterField.values()) {
            val bounds = fieldBounds(field)
            addField(panel, state, field, focusedField, bounds.x, bounds.y)
        }
    }

    @JvmStatic
    fun process(
        events: List<InputEventAPI>,
        root: CustomPanelAPI?,
        modalLeft: Float,
        modalTop: Float,
        state: StockReviewState,
        focusedField: StockReviewShipFilterField?,
    ): Result {
        if (root == null || root.position == null) {
            return Result(focusedField, false)
        }
        var focus = focusedField
        var changed = false
        for (event in events) {
            if (event.isConsumed) continue
            if (event.isLMBDownEvent) {
                val clickedField = fieldAt(root, modalLeft, modalTop, event)
                if (focus != clickedField) {
                    focus = clickedField
                    changed = true
                }
                if (clickedField != null) {
                    event.consume()
                }
                continue
            }
            if (focus == null || !event.isKeyDownEvent || event.isModifierKey || event.eventValue == Keyboard.KEY_ESCAPE) {
                continue
            }
            when (event.eventValue) {
                Keyboard.KEY_BACK -> {
                    state.backspaceShipFilterField(focus)
                    changed = true
                    event.consume()
                }
                Keyboard.KEY_DELETE -> {
                    state.setShipFilterField(focus, "")
                    changed = true
                    event.consume()
                }
                Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                    focus = null
                    changed = true
                    event.consume()
                }
                else -> {
                    val char = event.eventChar
                    if (!event.isCtrlDown && char.code >= 32) {
                        state.appendShipFilterField(focus, char)
                        changed = true
                        event.consume()
                    }
                }
            }
        }
        return Result(focus, changed)
    }

    @JvmStatic
    fun preferredHeight(pad: Float, footerGap: Float, footerHeight: Float): Float =
        FIELD_TOP + (fieldRows() - 1) * (ROW_HEIGHT + ROW_GAP) + FIELD_HEIGHT + footerGap + footerHeight + pad

    data class Result(
        val focusedField: StockReviewShipFilterField?,
        val changed: Boolean,
    )

    private fun addField(
        panel: CustomPanelAPI,
        state: StockReviewState,
        field: StockReviewShipFilterField,
        focusedField: StockReviewShipFilterField?,
        x: Float,
        y: Float,
    ) {
        WimGuiControls.addLabel(panel, field.label, StockReviewStyle.TEXT, x, y, LABEL_WIDTH, FIELD_HEIGHT, Alignment.LMID)
        val box = panel.createCustomPanel(
            FIELD_WIDTH,
            FIELD_HEIGHT,
            WimGuiPanelPlugin(if (field == focusedField) StockReviewStyle.CELL_BACKGROUND else StockReviewStyle.ACTION_BACKGROUND, StockReviewStyle.ROW_BORDER),
        )
        panel.addComponent(box).inTL(x + LABEL_WIDTH + 8f, y)
        val value = state.getShipFilterField(field).let { if (field == focusedField) "${it}_" else it }
        WimGuiControls.addLabel(box, value, StockReviewStyle.TEXT, 0f, 0f, FIELD_WIDTH, FIELD_HEIGHT, Alignment.MID)
    }

    private fun fieldAt(root: CustomPanelAPI, modalLeft: Float, modalTop: Float, event: InputEventAPI): StockReviewShipFilterField? =
        StockReviewShipFilterField.values().firstOrNull { field ->
            val bounds = fieldBounds(field)
            val screenLeft = root.position.x + modalLeft + bounds.x + LABEL_WIDTH + 8f
            val screenRight = screenLeft + FIELD_WIDTH
            val screenTop = root.position.y + root.position.height - modalTop - bounds.y
            val screenBottom = screenTop - FIELD_HEIGHT
            event.x >= screenLeft && event.x <= screenRight && event.y >= screenBottom && event.y <= screenTop
        }

    private fun fieldBounds(field: StockReviewShipFilterField): Bounds {
        val index = field.ordinal
        val row = index / 2
        val x = if (index % 2 == 0) LEFT_X else RIGHT_X
        return Bounds(x, FIELD_TOP + row * (ROW_HEIGHT + ROW_GAP))
    }

    private fun fieldRows(): Int =
        (StockReviewShipFilterField.values().size + 1) / 2

    private data class Bounds(
        val x: Float,
        val y: Float,
    )
}
