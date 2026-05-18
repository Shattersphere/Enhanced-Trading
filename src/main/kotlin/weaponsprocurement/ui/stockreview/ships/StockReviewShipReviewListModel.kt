package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionCells
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.rows.StockReviewListRow
import weaponsprocurement.ui.stockreview.rows.StockReviewRowLayout
import weaponsprocurement.ui.stockreview.rows.StockReviewRowSpecs

object StockReviewShipReviewListModel {
    @JvmStatic
    fun build(
        pendingTrades: List<StockReviewPendingShipTrade>?,
        layout: StockReviewRowLayout,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        if (pendingTrades.isNullOrEmpty()) {
            rows.add(StockReviewListRow.fromSpec(StockReviewRowSpecs.empty("No queued ship trades.")))
            return rows
        }
        addGroup(rows, "Buying", pendingTrades.filter { it.isBuy() }, layout, true)
        addGroup(rows, "Selling", pendingTrades.filter { !it.isBuy() }, layout, rows.isNotEmpty())
        return rows
    }

    private fun addGroup(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        label: String,
        trades: List<StockReviewPendingShipTrade>,
        layout: StockReviewRowLayout,
        topGap: Boolean,
    ) {
        rows.add(
            StockReviewListRow.fromSpec(
                StockReviewRowSpecs.staticHeading(
                    "$label [${trades.size}]",
                    topGap,
                    "Queued ship ${label.lowercase()} trades.",
                ),
            ),
        )
        if (trades.isEmpty()) {
            rows.add(StockReviewListRow.fromSpec(StockReviewRowSpecs.empty("No queued ${label.lowercase()} trades.")))
            return
        }
        for (trade in trades) {
            rows.add(row(trade, layout))
        }
    }

    private fun row(trade: StockReviewPendingShipTrade, layout: StockReviewRowLayout): WimGuiListRow<StockReviewAction> {
        val priceLabel = if (trade.isBuy()) {
            "Cost: ${StockReviewFormat.credits(trade.unitPrice.toLong())}"
        } else {
            "Gain: ${StockReviewFormat.credits(trade.unitPrice.toLong())}"
        }
        val cells = WimGuiRowCell.of(
            WimGuiRowCell.info<StockReviewAction>(priceLabel, 170f, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.MID),
            WimGuiRowCell.info<StockReviewAction>("Tariff: ${StockReviewFormat.credits(trade.tariffCredits.toLong())}", 130f, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.MID),
            StockReviewActionCells.standard(
                StockReviewActionGroup.PLAN_RESET,
                "Reset",
                StockReviewStyle.RESET_BUTTON_WIDTH,
                StockReviewStyle.ACTION_BACKGROUND,
                StockReviewAction.resetShipPlan(trade.recordKey),
                true,
                "Remove this ship from the pending plan.",
            ),
        )
        return StockReviewListRow.fromSpec(
            StockReviewRowSpecs.item(
                "${trade.memberName} - ${trade.hullLabel}",
                cells,
                null,
                null,
                null,
                layout.itemIndent,
                null,
            ),
        )
    }
}
