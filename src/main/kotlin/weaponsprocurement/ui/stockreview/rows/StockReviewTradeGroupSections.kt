package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrade
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import java.util.Collections

class StockReviewTradeGroupSection private constructor(
    @JvmField val tradeGroup: StockReviewTradeGroup,
    private val topGap: Boolean,
    private val includesWorstCaseRow: Boolean,
) {
    fun tradesFrom(pendingTrades: List<StockReviewPendingTrade>?): List<StockReviewPendingTrade> {
        val result = ArrayList<StockReviewPendingTrade>()
        if (pendingTrades == null) {
            return result
        }
        for (trade in pendingTrades) {
            if (matches(trade)) {
                result.add(trade)
            }
        }
        return result
    }

    fun addTo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot,
        groupTrades: List<StockReviewPendingTrade>,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ) {
        val expanded = state.isExpanded(tradeGroup)
        StockReviewListSection.add(
            rows,
            layout,
            StockReviewListSectionSpec(
                groupTrades,
                expanded,
                StockReviewTradeGroupHeadingRows.reviewGroup(
                    tradeGroup,
                    groupTrades.size,
                    expanded,
                    topGap,
                ),
                includesWorstCaseRow,
                StockReviewReviewTradeRowAppender(snapshot, state, tradeContext, layout),
            ),
        )
    }

    private fun matches(trade: StockReviewPendingTrade): Boolean =
        if (StockReviewTradeGroup.BUYING == tradeGroup) trade.isBuy() else trade.isSell()

    companion object {
        @JvmStatic
        fun buying(): StockReviewTradeGroupSection =
            StockReviewTradeGroupSection(StockReviewTradeGroup.BUYING, false, true)

        @JvmStatic
        fun selling(): StockReviewTradeGroupSection =
            StockReviewTradeGroupSection(StockReviewTradeGroup.SELLING, true, false)
    }
}

object StockReviewTradeGroupSections {
    @JvmField val ORDERED: List<StockReviewTradeGroupSection> = Collections.unmodifiableList(
        listOf(
            StockReviewTradeGroupSection.buying(),
            StockReviewTradeGroupSection.selling(),
        ),
    )

    @JvmStatic
    fun build(pendingTrades: List<StockReviewPendingTrade>?): StockReviewTradeGroupRows {
        val rows = ArrayList<StockReviewTradeGroupRow>()
        for (section in ORDERED) {
            rows.add(StockReviewTradeGroupRow(section, section.tradesFrom(pendingTrades)))
        }
        return StockReviewTradeGroupRows(rows)
    }
}

class StockReviewTradeGroupRows(private val rows: List<StockReviewTradeGroupRow>) {
    fun allEmpty(): Boolean {
        for (row in rows) {
            if (row.trades.isNotEmpty()) {
                return false
            }
        }
        return true
    }

    fun addTo(
        targetRows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ) {
        for (row in rows) {
            row.section.addTo(targetRows, snapshot, row.trades, state, tradeContext, layout)
        }
    }
}

class StockReviewTradeGroupRow(
    @JvmField val section: StockReviewTradeGroupSection,
    @JvmField val trades: List<StockReviewPendingTrade>,
)
