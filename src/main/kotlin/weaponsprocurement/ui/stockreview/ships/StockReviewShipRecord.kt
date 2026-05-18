package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI

class StockReviewShipRecord(
    @JvmField val key: String,
    @JvmField val side: StockReviewShipTradeSide,
    @JvmField val member: FleetMemberAPI,
    @JvmField val submarket: SubmarketAPI?,
    @JvmField val submarketId: String?,
    @JvmField val submarketName: String?,
    @JvmField val price: StockReviewShipPriceQuote,
    @JvmField val debugProfile: StockReviewShipDebugProfile? = null,
) {
    fun isBuy(): Boolean = side.isBuy()
    fun isSell(): Boolean = !side.isBuy()
    fun isDebug(): Boolean = debugProfile != null
    fun displayName(): String = member.shipName?.takeIf { it.isNotBlank() } ?: member.hullSpec?.hullName ?: "Unknown Ship"
    fun hullLabel(): String = member.hullSpec?.nameWithDesignationWithDashClass ?: member.hullSpec?.hullNameWithDashClass ?: member.specId
}

class StockReviewShipDebugProfile(
    @JvmField val hullClassLabel: String,
    @JvmField val sizeLabel: String,
    @JvmField val tooltipTitle: String,
    @JvmField val manufacturer: String,
    @JvmField val description: String,
    @JvmField val logisticsLeft: List<StockReviewShipDebugStat>,
    @JvmField val logisticsRight: List<StockReviewShipDebugStat>,
    @JvmField val combat: List<StockReviewShipDebugStat>,
    @JvmField val system: String,
    @JvmField val mounts: String,
    @JvmField val armaments: String,
    @JvmField val hullMods: String,
)

class StockReviewShipDebugStat(
    @JvmField val label: String,
    @JvmField val value: String,
)
