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

class StockReviewTradeSummaryField(
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
}

object StockReviewTradeSummaryFields {
    @JvmStatic
    fun build(tradeContext: StockReviewTradeContext, state: StockReviewState?): List<StockReviewTradeSummaryField> {
        val netCost = tradeContext.totalCost()
        val cargoDelta = tradeContext.totalCargoSpaceDelta()
        val warning = state?.getTradeWarning() ?: "None"
        return listOf(
            StockReviewTradeSummaryField(
                "Warning",
                warning,
                if (warning == "None") StockReviewStyle.CELL_BACKGROUND else StockReviewStyle.PRESET_SCOPE_BUTTON,
                "Most recent trade warning for credits or cargo capacity.",
            ),
            StockReviewTradeSummaryField(
                "Tariffs Paid",
                tariffsPaidLabel(tradeContext),
                if (tradeContext.totalMarkupPaid() > 0) StockReviewStyle.CANCEL_BUTTON else StockReviewStyle.CELL_BACKGROUND,
                StockReviewTooltips.tariffs(),
            ),
            StockReviewTradeSummaryField(
                "Credits Available",
                creditsAvailableLabel(tradeContext.credits(), netCost),
                creditDeltaFill(netCost),
                "Current credits plus the signed change from queued trades.",
            ),
            StockReviewTradeSummaryField(
                "Cargo Space Available",
                cargoAvailableLabel(tradeContext.cargoSpaceLeft(), cargoDelta),
                cargoDeltaFill(cargoDelta),
                "Current cargo space plus the signed cargo change from queued trades.",
            ),
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
