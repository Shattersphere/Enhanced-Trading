package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.fleet.FleetMemberAPI

object StockReviewShipEligibility {
    @JvmStatic
    fun isTradeableShip(member: FleetMemberAPI?): Boolean {
        if (member == null) return false
        if (member.isFighterWing) return false
        if (member.isStation) return false
        return member.type != null
    }

    @JvmStatic
    fun isSellableShip(member: FleetMemberAPI?): Boolean {
        return isTradeableShip(member) && member?.isFlagship != true
    }
}
