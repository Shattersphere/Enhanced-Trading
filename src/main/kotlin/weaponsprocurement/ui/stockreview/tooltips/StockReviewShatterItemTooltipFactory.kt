package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterWeaponTooltip
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterWingTooltip
import weaponsprocurement.stock.item.WeaponStockRecord

internal object StockReviewShatterItemTooltipFactory {
    fun forRecord(record: WeaponStockRecord): TooltipMakerAPI.TooltipCreator? {
        val itemId = record.itemId ?: return null
        val context = StockReviewItemTooltipContext(record).shatterContext()
        if (record.isWing()) {
            val spec = record.wingSpec ?: return null
            return ShatterWingTooltip(
                wingId = itemId,
                spec = spec,
                includeReplacementNotes = false,
                context = context,
            )
        }
        val spec = record.spec ?: return null
        return ShatterWeaponTooltip(
            weaponId = itemId,
            spec = spec,
            context = context,
        )
    }
}
