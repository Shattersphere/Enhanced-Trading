package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle

class StockReviewListSection<T> private constructor(
    private val items: List<T>,
    private val expanded: Boolean,
    private val heading: () -> WimGuiListRow<StockReviewAction>,
    private val includeWorstCaseRow: Boolean,
    private val itemAppender: (MutableList<WimGuiListRow<StockReviewAction>>, T) -> Unit,
) {
    fun addTo(rows: MutableList<WimGuiListRow<StockReviewAction>>, layout: StockReviewRowLayout): Int {
        rows.add(heading.invoke())
        if (!expanded) {
            return items.size
        }
        if (StockReviewStyle.SHOW_WIDTH_TEST_ROWS && includeWorstCaseRow) {
            StockReviewWorstCaseItemRows.add(rows, layout)
        }
        for (item in items) {
            itemAppender.invoke(rows, item)
        }
        return items.size
    }

    companion object {
        @JvmStatic
        fun <T> builder(items: List<T>): Builder<T> = Builder(items)

        @JvmStatic
        fun addHeading(
            rows: MutableList<WimGuiListRow<StockReviewAction>>,
            heading: WimGuiListRow<StockReviewAction>,
        ) {
            rows.add(heading)
        }
    }

    class Builder<T>(private val items: List<T>) {
        private var expanded: Boolean = true
        private var heading: (() -> WimGuiListRow<StockReviewAction>)? = null
        private var includeWorstCaseRow: Boolean = false
        private var itemAppender: ((MutableList<WimGuiListRow<StockReviewAction>>, T) -> Unit)? = null

        fun expanded(value: Boolean) = apply { expanded = value }
        fun heading(value: () -> WimGuiListRow<StockReviewAction>) = apply { heading = value }
        fun includeWorstCaseRow(value: Boolean) = apply { includeWorstCaseRow = value }
        fun itemAppender(value: (MutableList<WimGuiListRow<StockReviewAction>>, T) -> Unit) = apply { itemAppender = value }

        fun build(): StockReviewListSection<T> =
            StockReviewListSection(
                items,
                expanded,
                heading ?: throw IllegalStateException("Stock-review list section heading is required."),
                includeWorstCaseRow,
                itemAppender ?: throw IllegalStateException("Stock-review list section item appender is required."),
            )
    }
}
