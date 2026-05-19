package weaponsprocurement.stock.item

/**
 * Debug-only item profile used to feed worst-case values through the same row and tooltip
 * code as real weapons/LPCs without inventing a parallel debug renderer.
 */
class StockDebugItemProfile(
    @JvmField val iconSpriteName: String,
    @JvmField val tooltipTitle: String,
    @JvmField val manufacturer: String,
    @JvmField val description: String,
    @JvmField val cargoSpaceLabel: String?,
    @JvmField val priceLabel: String?,
    @JvmField val primaryRows: List<StockDebugItemStat>,
    @JvmField val ancillaryRows: List<StockDebugItemStat>,
    @JvmField val wingTechnicalRows: List<StockDebugItemStat>,
    @JvmField val wingSystem: String,
    @JvmField val wingArmaments: String,
    private val fieldValues: Map<String, String>,
) {
    fun value(key: String): String? = fieldValues[key]
}

class StockDebugItemStat(
    @JvmField val label: String,
    @JvmField val value: String,
)
