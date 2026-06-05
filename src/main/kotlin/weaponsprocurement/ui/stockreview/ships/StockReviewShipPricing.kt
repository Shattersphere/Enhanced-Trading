package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI

object StockReviewShipPricing {
    @JvmStatic
    fun buyQuote(member: FleetMemberAPI?, submarket: SubmarketAPI?): StockReviewShipPriceQuote {
        val base = roundCredit(member?.baseBuyValue ?: 0f)
        val tariff = tariffCredits(base, submarket?.tariff ?: 0f)
        return StockReviewShipPriceQuote(base, tariff, addCredits(base, tariff), submarket?.tariff ?: 0f)
    }

    @JvmStatic
    fun sellQuote(member: FleetMemberAPI?, submarket: SubmarketAPI?): StockReviewShipPriceQuote {
        val base = roundCredit(member?.baseSellValue ?: 0f)
        val tariff = tariffCredits(base, submarket?.tariff ?: 0f)
        return StockReviewShipPriceQuote(base, tariff, Math.max(0, base - tariff), submarket?.tariff ?: 0f)
    }

    private fun tariffCredits(base: Int, tariffRate: Float): Int =
        Math.round(Math.max(0f, base * Math.max(0f, tariffRate)))

    private fun roundCredit(value: Float): Int =
        Math.max(0, Math.round(Math.max(0f, value)))

    private fun addCredits(left: Int, right: Int): Int =
        Math.min(Int.MAX_VALUE.toLong(), left.toLong() + right.toLong()).toInt()
}

class StockReviewShipPriceQuote(
    @JvmField val baseCredits: Int,
    @JvmField val tariffCredits: Int,
    @JvmField val finalCredits: Int,
    @JvmField val tariffRate: Float,
)
