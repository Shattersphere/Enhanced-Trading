package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState

interface StockReviewSectionRowAppender<T> {
    fun add(rows: MutableList<WimGuiListRow<StockReviewAction>>, item: T)
}

class StockReviewListSectionSpec<T>(
    @JvmField val items: List<T>,
    @JvmField val expanded: Boolean,
    @JvmField val heading: WimGuiListRow<StockReviewAction>,
    @JvmField val includeWorstCaseRow: Boolean,
    @JvmField val worstCaseItemType: StockItemType?,
    @JvmField val state: StockReviewState?,
    @JvmField val itemAppender: StockReviewSectionRowAppender<T>,
)

class StockReviewListSection<T> private constructor(
    private val spec: StockReviewListSectionSpec<T>,
) {
    fun addTo(rows: MutableList<WimGuiListRow<StockReviewAction>>, layout: StockReviewRowLayout): Int {
        rows.add(spec.heading)
        if (!spec.expanded) {
            return spec.items.size
        }
        if (StockReviewStyle.showDebugUi() && spec.includeWorstCaseRow) {
            StockReviewWorstCaseItemRows.add(rows, layout, spec.worstCaseItemType ?: StockItemType.WEAPON, spec.state)
        }
        for (item in spec.items) {
            spec.itemAppender.add(rows, item)
        }
        return spec.items.size
    }

    companion object {
        @JvmStatic
        fun <T> add(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            layout: StockReviewRowLayout,
            spec: StockReviewListSectionSpec<T>,
        ): Int = StockReviewListSection(spec).addTo(rows, layout)

        @JvmStatic
        fun addHeading(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            heading: WimGuiListRow<StockReviewAction>,
        ) {
            rows.add(heading)
        }
    }
}
