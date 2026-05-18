package weaponsprocurement.ui.stockreview.trade

import weaponsprocurement.stock.item.StockItemType

enum class StockReviewReviewItemGroup(
    val label: String,
    val itemType: StockItemType?,
) {
    WEAPONS("Weapons", StockItemType.WEAPON),
    WINGS("Wings", StockItemType.WING),
    HULLMODS("Hullmods", null);

    companion object {
        @JvmField
        val ORDERED: List<StockReviewReviewItemGroup> = listOf(WEAPONS, WINGS, HULLMODS)

        @JvmStatic
        fun fromItemKey(itemKey: String?): StockReviewReviewItemGroup {
            return if (StockItemType.fromKey(itemKey) == StockItemType.WING) WINGS else WEAPONS
        }
    }
}
