package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeContext
import java.util.Collections

class StockReviewItemTypeSection private constructor(
    @JvmField val itemType: StockItemType,
    private val topGap: Boolean,
) {
    fun addTo(
        rows: MutableList<WimGuiListRow<StockReviewAction>>,
        snapshot: WeaponStockSnapshot?,
        state: StockReviewState,
        tradeContext: StockReviewTradeContext,
        layout: StockReviewRowLayout,
    ): Int {
        val count = snapshot?.getCount(itemType) ?: 0
        val expanded = state.isExpanded(itemType)
        StockReviewListSection.addHeading(rows, StockReviewHeadingRows.itemType(itemType, count, expanded, topGap))
        if (!expanded) {
            return count
        }

        var displayed = 0
        for (categorySection in StockReviewStockCategorySections.ORDERED) {
            displayed += categorySection.addTo(rows, snapshot, state, tradeContext, layout, itemType)
        }
        return displayed
    }

    companion object {
        @JvmStatic
        fun weapons(): StockReviewItemTypeSection = StockReviewItemTypeSection(StockItemType.WEAPON, false)

        @JvmStatic
        fun wings(): StockReviewItemTypeSection = StockReviewItemTypeSection(StockItemType.WING, true)
    }
}

object StockReviewItemTypeSections {
    @JvmField val ORDERED: List<StockReviewItemTypeSection> = Collections.unmodifiableList(
        listOf(
            StockReviewItemTypeSection.weapons(),
            StockReviewItemTypeSection.wings(),
        ),
    )
}
