package weaponsprocurement.ui.stockreview.controls

import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.WimGuiSemanticButtonFactory
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import java.awt.Color

fun interface StockReviewButtonValue<C, T> {
    fun resolve(context: C): T
}

class StockReviewButtonDefinition<C>(
    @JvmField val id: String,
    @JvmField val group: StockReviewActionGroup,
    @JvmField val width: Float,
    private val label: StockReviewButtonValue<C, String?>,
    private val action: StockReviewButtonValue<C, StockReviewAction>,
    private val enabled: StockReviewButtonValue<C, Boolean>,
    private val fillColor: StockReviewButtonValue<C, Color>,
    private val tooltip: StockReviewButtonValue<C, String?>,
) {
    fun build(context: C, factory: StockReviewActionButtonFactory): WimGuiButtonSpec<StockReviewAction> =
        factory.button(
            group,
            width,
            label.resolve(context),
            action.resolve(context),
            enabled.resolve(context),
            fillColor.resolve(context),
            tooltip.resolve(context),
        )

    companion object {
        @JvmStatic
        fun <C> static(
            id: String,
            group: StockReviewActionGroup,
            width: Float,
            label: String,
            action: StockReviewAction,
            fillColor: Color,
            tooltip: String,
        ): StockReviewButtonDefinition<C> =
            StockReviewButtonDefinition(
                id,
                group,
                width,
                constant(label),
                constant(action),
                constant(true),
                constant(fillColor),
                constant(tooltip),
            )

        @JvmStatic
        fun <C> staticWithEnabled(
            id: String,
            group: StockReviewActionGroup,
            width: Float,
            label: String,
            action: StockReviewAction,
            enabled: StockReviewButtonValue<C, Boolean>,
            fillColor: Color,
            tooltip: String,
        ): StockReviewButtonDefinition<C> =
            StockReviewButtonDefinition(
                id,
                group,
                width,
                constant(label),
                constant(action),
                enabled,
                constant(fillColor),
                constant(tooltip),
            )

        @JvmStatic
        fun <C> dynamic(
            id: String,
            group: StockReviewActionGroup,
            width: Float,
            label: StockReviewButtonValue<C, String?>,
            action: StockReviewButtonValue<C, StockReviewAction>,
            enabled: StockReviewButtonValue<C, Boolean>,
            fillColor: StockReviewButtonValue<C, Color>,
            tooltip: StockReviewButtonValue<C, String?>,
        ): StockReviewButtonDefinition<C> =
            StockReviewButtonDefinition(id, group, width, label, action, enabled, fillColor, tooltip)

        @JvmStatic
        fun <C> constant(value: String?): StockReviewButtonValue<C, String?> =
            StockReviewButtonValue { value }

        @JvmStatic
        fun <C> constant(value: StockReviewAction): StockReviewButtonValue<C, StockReviewAction> =
            StockReviewButtonValue { value }

        @JvmStatic
        fun <C> constant(value: Boolean): StockReviewButtonValue<C, Boolean> =
            StockReviewButtonValue { value }

        @JvmStatic
        fun <C> constant(value: Color): StockReviewButtonValue<C, Color> =
            StockReviewButtonValue { value }
    }
}

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
        StockReviewActionGuards.requireGroup(group, action)
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

class StockReviewActionRef private constructor(
    @JvmField val group: StockReviewActionGroup,
    @JvmField val action: StockReviewAction,
) {
    companion object {
        @JvmStatic
        fun of(group: StockReviewActionGroup, action: StockReviewAction): StockReviewActionRef {
            StockReviewActionGuards.requireGroup(group, action)
            return StockReviewActionRef(group, action)
        }

        @JvmStatic
        fun rowExpansion(action: StockReviewAction): StockReviewActionRef = of(StockReviewActionGroup.ROW_EXPANSION, action)

        @JvmStatic
        fun filters(action: StockReviewAction): StockReviewActionRef = of(StockReviewActionGroup.FILTERS, action)

        @JvmStatic
        fun debugMode(action: StockReviewAction): StockReviewActionRef = of(StockReviewActionGroup.DEBUG_MODE, action)

        @JvmStatic
        fun scroll(action: StockReviewAction): StockReviewActionRef = of(StockReviewActionGroup.SCROLL, action)
    }
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
            StockReviewActionGuards.requireGroup(group, action)
            return WimGuiRowCell.standardAction(label, width, fillColor, action, enabled, tooltip)
        }
    }
}

object StockReviewActionGuards {
    @JvmStatic
    fun requireGroup(expected: StockReviewActionGroup, action: StockReviewAction) {
        val actual = action.getGroup()
        require(actual == expected) {
            "Stock-review action ${action.getType()} belongs to ${actual.name}, not ${expected.name}"
        }
    }
}
