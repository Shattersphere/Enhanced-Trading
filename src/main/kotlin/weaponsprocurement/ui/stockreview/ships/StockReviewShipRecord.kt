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
) {
    fun isBuy(): Boolean = side.isBuy()
    fun isSell(): Boolean = !side.isBuy()
    fun displayName(): String = member.shipName?.takeIf { it.isNotBlank() } ?: member.hullSpec?.hullName ?: "Unknown Ship"
    fun hullLabel(): String = member.hullSpec?.nameWithDesignationWithDashClass ?: member.hullSpec?.hullNameWithDashClass ?: member.specId
}
