package weaponsprocurement.ui.stockreview.ships

class StockReviewPendingShipTrade private constructor(
    @JvmField val recordKey: String,
    @JvmField val memberId: String,
    @JvmField val side: StockReviewShipTradeSide,
    @JvmField val submarketId: String?,
    @JvmField val memberName: String,
    @JvmField val hullLabel: String,
    @JvmField val unitPrice: Int,
    @JvmField val basePrice: Int,
    @JvmField val tariffCredits: Int,
) {
    fun isBuy(): Boolean = side.isBuy()
    fun copy(): StockReviewPendingShipTrade =
        StockReviewPendingShipTrade(recordKey, memberId, side, submarketId, memberName, hullLabel, unitPrice, basePrice, tariffCredits)

    companion object {
        @JvmStatic
        fun fromRecord(record: StockReviewShipRecord?): StockReviewPendingShipTrade? {
            if (record == null) return null
            val memberId = record.member.id ?: return null
            if (memberId.isBlank()) return null
            return StockReviewPendingShipTrade(
                record.key,
                memberId,
                record.side,
                record.submarketId,
                record.displayName(),
                record.hullLabel(),
                record.price.finalCredits,
                record.price.baseCredits,
                record.price.tariffCredits,
            )
        }
    }
}
