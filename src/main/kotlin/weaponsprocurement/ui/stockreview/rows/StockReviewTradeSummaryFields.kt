package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.ui.WimGuiControls
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewQuoteBook
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import java.awt.Color
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class StockReviewTradeSummaryField private constructor(
    @JvmField val label: String,
    @JvmField val value: String,
    @JvmField val valueFill: Color,
    @JvmField val tooltip: String,
) {
    fun render(root: CustomPanelAPI, width: Float, y: Float) {
        WimGuiControls.addLabelTextRow(
            root,
            StockReviewStyle.PAD,
            y,
            width,
            StockReviewStyle.ROW_HEIGHT,
            label,
            value,
            valueFill,
            StockReviewStyle.ROW_BORDER,
            StockReviewStyle.TEXT,
            tooltip,
        )
    }

    companion object {
        @JvmStatic
        fun warning(value: String): StockReviewTradeSummaryField =
            StockReviewTradeSummaryField(
                "Warning",
                value,
                if (value == "None") StockReviewStyle.CELL_BACKGROUND else StockReviewStyle.PRESET_SCOPE_BUTTON,
                "Most recent trade warning for credits or cargo capacity.",
            )

        @JvmStatic
        fun tariffsPaid(value: String, markupPaid: Long): StockReviewTradeSummaryField =
            StockReviewTradeSummaryField(
                "Tariffs Paid",
                value,
                if (markupPaid > 0) StockReviewStyle.CANCEL_BUTTON else StockReviewStyle.CELL_BACKGROUND,
                StockReviewTooltips.tariffs(),
            )

        @JvmStatic
        fun creditsAvailable(value: String, netCost: Long): StockReviewTradeSummaryField =
            StockReviewTradeSummaryField(
                "Credits Available",
                value,
                creditDeltaFill(netCost),
                "Current credits plus the signed change from queued trades.",
            )

        @JvmStatic
        fun cargoSpaceAvailable(value: String, cargoDelta: Float): StockReviewTradeSummaryField =
            StockReviewTradeSummaryField(
                "Cargo Space Available",
                value,
                cargoDeltaFill(cargoDelta),
                "Current cargo space plus the signed cargo change from queued trades.",
            )

        private fun creditDeltaFill(netCost: Long): Color {
            if (netCost == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
                return StockReviewStyle.CANCEL_BUTTON
            }
            if (netCost > 0) {
                return StockReviewStyle.CANCEL_BUTTON
            }
            if (netCost < 0) {
                return StockReviewStyle.CONFIRM_BUTTON
            }
            return StockReviewStyle.CELL_BACKGROUND
        }

        private fun cargoDeltaFill(cargoDelta: Float): Color {
            if (cargoDelta > 0.01f) {
                return StockReviewStyle.CANCEL_BUTTON
            }
            if (cargoDelta < -0.01f) {
                return StockReviewStyle.CONFIRM_BUTTON
            }
            return StockReviewStyle.CELL_BACKGROUND
        }
    }
}

object StockReviewTradeSummaryFields {
    @JvmStatic
    fun build(tradeContext: StockReviewTradeContext, state: StockReviewState?): List<StockReviewTradeSummaryField> {
        val netCost = tradeContext.totalCost()
        val cargoDelta = tradeContext.totalCargoSpaceDelta()
        val warning = state?.getTradeWarning() ?: "None"
        return listOf(
            StockReviewTradeSummaryField.warning(warning),
            StockReviewTradeSummaryField.tariffsPaid(tariffsPaidLabel(tradeContext), tradeContext.totalMarkupPaid()),
            StockReviewTradeSummaryField.creditsAvailable(creditsAvailableLabel(tradeContext.credits(), netCost), netCost),
            StockReviewTradeSummaryField.cargoSpaceAvailable(cargoAvailableLabel(tradeContext.cargoSpaceLeft(), cargoDelta), cargoDelta),
        )
    }

    private fun formatCargo(value: Float): String {
        val rounded = value.roundToInt().toFloat()
        if (abs(value - rounded) < 0.05f) {
            return rounded.roundToInt().toString()
        }
        return String.format(Locale.US, "%.1f", value)
    }

    private fun creditsAvailableLabel(creditsAvailable: Float, netCost: Long): String {
        if (netCost == StockReviewQuoteBook.PRICE_UNAVAILABLE.toLong()) {
            return "${StockReviewFormat.credits(Math.round(creditsAvailable).toLong())} [?]"
        }
        return "${StockReviewFormat.credits(Math.round(creditsAvailable).toLong())} [${signedCredits(-netCost)}]"
    }

    private fun cargoAvailableLabel(cargoSpaceAvailable: Float, cargoDelta: Float): String =
        "${formatCargo(cargoSpaceAvailable)} [${signedCargo(-cargoDelta)}]"

    private fun tariffsPaidLabel(tradeContext: StockReviewTradeContext): String {
        val markup = tradeContext.totalMarkupPaid()
        val totalBuyCost = tradeContext.totalBuyCost()
        val percent = if (markup <= 0 || totalBuyCost <= 0) {
            0
        } else {
            Math.round(markup.toDouble() * 100.0 / totalBuyCost.toDouble()).toInt()
        }
        return "${StockReviewFormat.credits(markup)} [$percent%]"
    }

    private fun signedCredits(delta: Long): String = (if (delta >= 0) "+" else "-") + StockReviewFormat.credits(delta)

    private fun signedCargo(delta: Float): String = (if (delta >= 0f) "+" else "-") + formatCargo(abs(delta))

}
