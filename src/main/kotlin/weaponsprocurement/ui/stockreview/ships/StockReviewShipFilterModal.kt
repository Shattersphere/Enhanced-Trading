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

object StockReviewShipFilterModal {
    private const val FIELD_WIDTH = 160f
    private const val FIELD_HEIGHT = 22f
    private const val LABEL_WIDTH = 160f
    private const val LEFT_X = 20f
    private const val RIGHT_X = 368f
    private const val SIZE_TOP = 88f
    private const val FIELD_TOP = 154f
    private const val ROW_GAP = 10f
    private const val ROW_HEIGHT = 28f

    @JvmStatic
    fun render(
        panel: CustomPanelAPI,
        state: StockReviewState,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        addSectionLabel(panel, "Ship size", LEFT_X, 62f, 646f)
        var x = LEFT_X
        for (size in StockReviewShipSizeFilter.values()) {
            val active = state.isShipSizeFilterActive(size)
            WimGuiControls.addBoundButton(
                panel,
                x,
                SIZE_TOP,
                FIELD_HEIGHT,
                WimGuiButtonSpec.toggle(
                    154f,
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
            x += 164f
        }

        addField(panel, state, StockReviewShipFilterField.MAX_COST, LEFT_X, FIELD_TOP)
        addField(panel, state, StockReviewShipFilterField.MIN_ORDNANCE_POINTS, RIGHT_X, FIELD_TOP)
        addField(panel, state, StockReviewShipFilterField.MIN_SMALL_MOUNTS, LEFT_X, FIELD_TOP + ROW_HEIGHT + ROW_GAP)
        addField(panel, state, StockReviewShipFilterField.MIN_MEDIUM_MOUNTS, RIGHT_X, FIELD_TOP + ROW_HEIGHT + ROW_GAP)
        addField(panel, state, StockReviewShipFilterField.MIN_LARGE_MOUNTS, LEFT_X, FIELD_TOP + 2f * (ROW_HEIGHT + ROW_GAP))
        addField(panel, state, StockReviewShipFilterField.MIN_ENERGY_MOUNTS, RIGHT_X, FIELD_TOP + 2f * (ROW_HEIGHT + ROW_GAP))
        addField(panel, state, StockReviewShipFilterField.MIN_BALLISTIC_MOUNTS, LEFT_X, FIELD_TOP + 3f * (ROW_HEIGHT + ROW_GAP))
        addField(panel, state, StockReviewShipFilterField.SHIP_SYSTEM, RIGHT_X, FIELD_TOP + 3f * (ROW_HEIGHT + ROW_GAP))
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
    fun preferredHeight(): Float = 390f

    data class Result(
        val focusedField: StockReviewShipFilterField?,
        val changed: Boolean,
    )

    private fun addField(panel: CustomPanelAPI, state: StockReviewState, field: StockReviewShipFilterField, x: Float, y: Float) {
        WimGuiControls.addLabel(panel, field.label, StockReviewStyle.TEXT, x, y, LABEL_WIDTH, FIELD_HEIGHT, Alignment.LMID)
        val box = panel.createCustomPanel(FIELD_WIDTH, FIELD_HEIGHT, WimGuiPanelPlugin(StockReviewStyle.ACTION_BACKGROUND, StockReviewStyle.ROW_BORDER))
        panel.addComponent(box).inTL(x + LABEL_WIDTH + 8f, y)
        WimGuiControls.addLabel(box, state.getShipFilterField(field), StockReviewStyle.TEXT, 0f, 0f, FIELD_WIDTH, FIELD_HEIGHT, Alignment.MID)
    }

    private fun addSectionLabel(panel: CustomPanelAPI, label: String, x: Float, y: Float, width: Float) {
        val strip = panel.createCustomPanel(width, FIELD_HEIGHT, WimGuiPanelPlugin(StockReviewStyle.ACTION_BACKGROUND, StockReviewStyle.ROW_BORDER))
        panel.addComponent(strip).inTL(x, y)
        WimGuiControls.addLabel(strip, label, StockReviewStyle.TEXT, 0f, 0f, width, FIELD_HEIGHT, Alignment.MID)
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

    private fun fieldBounds(field: StockReviewShipFilterField): Bounds =
        when (field) {
            StockReviewShipFilterField.MAX_COST -> Bounds(LEFT_X, FIELD_TOP)
            StockReviewShipFilterField.MIN_ORDNANCE_POINTS -> Bounds(RIGHT_X, FIELD_TOP)
            StockReviewShipFilterField.MIN_SMALL_MOUNTS -> Bounds(LEFT_X, FIELD_TOP + ROW_HEIGHT + ROW_GAP)
            StockReviewShipFilterField.MIN_MEDIUM_MOUNTS -> Bounds(RIGHT_X, FIELD_TOP + ROW_HEIGHT + ROW_GAP)
            StockReviewShipFilterField.MIN_LARGE_MOUNTS -> Bounds(LEFT_X, FIELD_TOP + 2f * (ROW_HEIGHT + ROW_GAP))
            StockReviewShipFilterField.MIN_ENERGY_MOUNTS -> Bounds(RIGHT_X, FIELD_TOP + 2f * (ROW_HEIGHT + ROW_GAP))
            StockReviewShipFilterField.MIN_BALLISTIC_MOUNTS -> Bounds(LEFT_X, FIELD_TOP + 3f * (ROW_HEIGHT + ROW_GAP))
            StockReviewShipFilterField.SHIP_SYSTEM -> Bounds(RIGHT_X, FIELD_TOP + 3f * (ROW_HEIGHT + ROW_GAP))
        }

    private data class Bounds(
        val x: Float,
        val y: Float,
    )
}
