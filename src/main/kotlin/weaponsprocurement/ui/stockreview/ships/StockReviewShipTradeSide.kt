package weaponsprocurement.ui.stockreview.ships

enum class StockReviewShipTradeSide {
    BUY,
    SELL;

    fun isBuy(): Boolean = this == BUY
}
