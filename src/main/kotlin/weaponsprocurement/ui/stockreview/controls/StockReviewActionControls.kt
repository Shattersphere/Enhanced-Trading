package weaponsprocurement.ui.stockreview.controls

import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.WimGuiSemanticButtonFactory
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import java.awt.Color

class StockReviewActionButtonFactory(borderColor: Color) {
    private val delegate = WimGuiSemanticButtonFactory<StockReviewAction>(borderColor)

    fun button(
        group: StockReviewActionGroup,
        width: Float,
        label: String?,
        action: StockReviewAction,
        enabled: Boolean,
        fillColor: Color,
    ): WimGuiButtonSpec<StockReviewAction> = button(group, width, label, action, enabled, fillColor, null)

    fun button(
        group: StockReviewActionGroup,
        width: Float,
        label: String?,
        action: StockReviewAction,
        enabled: Boolean,
        fillColor: Color,
        tooltip: String?,
    ): WimGuiButtonSpec<StockReviewAction> {
        requireActionGroup(group, action)
        return delegate.button(width, label, action, enabled, fillColor, tooltip)
    }

    fun enabledButton(
        group: StockReviewActionGroup,
        width: Float,
        label: String?,
        action: StockReviewAction,
        fillColor: Color,
    ): WimGuiButtonSpec<StockReviewAction> = enabledButton(group, width, label, action, fillColor, null)

    fun enabledButton(
        group: StockReviewActionGroup,
        width: Float,
        label: String?,
        action: StockReviewAction,
        fillColor: Color,
        tooltip: String?,
    ): WimGuiButtonSpec<StockReviewAction> = button(group, width, label, action, true, fillColor, tooltip)
}

class StockReviewActionCells private constructor() {
    companion object {
        @JvmStatic
        fun standard(
            group: StockReviewActionGroup,
            label: String?,
            width: Float,
            fillColor: Color?,
            action: StockReviewAction,
            enabled: Boolean,
        ): WimGuiRowCell<StockReviewAction> = standard(group, label, width, fillColor, action, enabled, null)

        @JvmStatic
        fun standard(
            group: StockReviewActionGroup,
            label: String?,
            width: Float,
            fillColor: Color?,
            action: StockReviewAction,
            enabled: Boolean,
            tooltip: String?,
        ): WimGuiRowCell<StockReviewAction> {
            requireActionGroup(group, action)
            return WimGuiRowCell.standardAction(label, width, fillColor, action, enabled, tooltip)
        }
    }
}

private fun requireActionGroup(expected: StockReviewActionGroup, action: StockReviewAction) {
    val actual = action.getGroup()
    require(actual == expected) {
        "Stock-review action ${action.getType()} belongs to ${actual.name}, not ${expected.name}"
    }
}
