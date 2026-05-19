package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.ui.WimGuiTooltip
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import weaponsprocurement.stock.item.WeaponStockRecord

class StockReviewRowIcon private constructor(
    val spriteName: String,
) {
    companion object {
        @JvmStatic
        fun weapon(spriteName: String?, motifType: WeaponAPI.WeaponType?): StockReviewRowIcon? {
            val resolvedSpriteName = spriteName?.takeIf { WimGuiTooltip.hasText(it) } ?: return null
            return StockReviewRowIcon(resolvedSpriteName)
        }

        @JvmStatic
        fun weapon(record: WeaponStockRecord?): StockReviewRowIcon? {
            record?.debugProfile?.iconSpriteName?.let { return StockReviewRowIcon(it) }
            if (record == null || record.spec == null) return null
            return weapon(
                StockReviewWeaponIconPlugin.spriteName(record.spec),
                StockReviewWeaponIconPlugin.motifType(record.spec),
            )
        }

        @JvmStatic
        fun wing(record: WeaponStockRecord?): StockReviewRowIcon? {
            record?.debugProfile?.iconSpriteName?.let { return StockReviewRowIcon(it) }
            val spriteName = record?.wingSpec?.variant?.hullSpec?.spriteName
            val resolvedSpriteName = spriteName?.takeIf { WimGuiTooltip.hasText(it) } ?: return null
            return StockReviewRowIcon(resolvedSpriteName)
        }

        @JvmStatic
        fun item(record: WeaponStockRecord?): StockReviewRowIcon? {
            if (record == null) return null
            return if (record.isWing()) wing(record) else weapon(record)
        }
    }
}
