package weaponsprocurement.ui.stockreview.state

enum class StockReviewTradeKind(val label: String) {
    ITEMS("Items"),
    SHIPS("Ships");

    fun next(): StockReviewTradeKind =
        if (this == ITEMS) SHIPS else ITEMS
}
