package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterWeaponTooltip
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterWingTooltip
import weaponsprocurement.stock.item.WeaponStockRecord

/**
 * Custom weapon/LPC tooltip approximation for debug and stress records.
 */
class StockReviewItemTooltip private constructor(
    private val record: WeaponStockRecord,
) : TooltipMakerAPI.TooltipCreator {
    private val itemContext = StockReviewItemTooltipContext(record)

    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float =
        if (record.isWing()) StockReviewWingTooltipRenderer.WIDTH else StockReviewWeaponTooltipRenderer.WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        if (record.isWing()) {
            addPaddedWingTooltip(tooltip)
        } else {
            StockReviewWeaponTooltipRenderer.addTooltip(tooltip, record, itemContext)
        }
    }

    private fun addPaddedWingTooltip(tooltip: TooltipMakerAPI) {
        val layout = record.debugProfile
            ?.takeIf { record.isWing() }
            ?.let { StockReviewWingTooltipLayoutBuilder.forDebugProfile(it) }
            ?: StockReviewWingTooltipLayoutBuilder.forRecord(record, record.wingSpec ?: return)
        StockReviewWingTooltipRenderer.addTooltip(tooltip, layout, record.ownedCount, itemContext.priceLabel())
    }

    companion object {
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun forRecord(record: WeaponStockRecord?, toggleText: String?): TooltipMakerAPI.TooltipCreator? {
            if (record == null) {
                return null
            }
            if (record.isDebug()) {
                return StockReviewItemTooltip(record)
            }
            if (record.isWing() && record.wingSpec == null) {
                return null
            }
            if (!record.isWing() && record.spec == null) {
                return null
            }
            val itemId = record.itemId ?: return null
            val context = StockReviewItemTooltipContext(record)
            if (record.isWing()) {
                val spec = record.wingSpec ?: return null
                return ShatterWingTooltip(
                    wingId = itemId,
                    spec = spec,
                    includeReplacementNotes = false,
                    context = context.shatterContext(),
                )
            }
            val spec = record.spec ?: return null
            return ShatterWeaponTooltip(
                weaponId = itemId,
                spec = spec,
                context = context.shatterContext(),
            )
        }
    }
}
