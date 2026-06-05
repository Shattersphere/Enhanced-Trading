package weaponsprocurement.ui.stockreview.tooltips

import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterItemTooltipContext
import com.shattersphere.shatterlib.starsector.ui.tooltip.ShatterTooltipContextLine
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.stock.item.SubmarketWeaponStock
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.trade.quote.CreditFormat
import java.util.Locale

internal data class StockReviewItemContextLine(
    val text: String,
    val highlight: String,
)

internal class StockReviewItemTooltipContext(
    private val record: WeaponStockRecord,
) {
    fun weaponCargoLines(): List<StockReviewItemContextLine> {
        val lines = ArrayList<StockReviewItemContextLine>()
        cargoSpaceLabel()?.takeIf { hasText(it) }?.let { cargoSpace ->
            lines.add(StockReviewItemContextLine("Cargo space: $cargoSpace per unit.", cargoSpace))
        }
        priceLabel()?.takeIf { hasText(it) }?.let { price ->
            lines.add(StockReviewItemContextLine("Price: $price per unit.", price))
        }
        val count = record.ownedCount.toString()
        val plural = if (record.ownedCount == 1) "weapon" else "weapons"
        lines.add(StockReviewItemContextLine("You own a total of $count $plural of this type.", count))
        return lines
    }

    fun cargoSpaceLabel(): String? {
        record.debugProfile?.cargoSpaceLabel?.let { return it }
        val cargoSpace = unitCargoSpace()
        return if (validNumber(cargoSpace)) formatOneDecimalTrim(cargoSpace) else null
    }

    fun priceLabel(): String? {
        record.debugProfile?.priceLabel?.let { return it }
        var price = record.cheapestPurchasableUnitPrice
        if (price == Int.MAX_VALUE) {
            price = StockItemStacks.referenceBaseUnitPrice(record.itemType, record.itemId)
            if (price <= 0) {
                price = Math.round(maxOf(0f, record.spec?.baseValue ?: 0f))
            }
        }
        return if (price <= 0) null else CreditFormat.credits(price)
    }

    fun shatterContext(): ShatterItemTooltipContext {
        val lines = ArrayList<ShatterTooltipContextLine>()
        if (!record.isWing()) {
            cargoSpaceLabel()?.takeIf { hasText(it) }?.let { cargoSpace ->
                lines.add(ShatterTooltipContextLine("Cargo space: $cargoSpace per unit.", cargoSpace))
            }
        }
        priceLabel()?.takeIf { hasText(it) }?.let { price ->
            val text = if (record.isWing()) "Sells for: $price per unit." else "Price: $price per unit."
            lines.add(ShatterTooltipContextLine(text, price))
        }
        val count = record.ownedCount.toString()
        val label = if (record.isWing()) {
            "fighter LPCs"
        } else if (record.ownedCount == 1) {
            "weapon"
        } else {
            "weapons"
        }
        lines.add(ShatterTooltipContextLine("You own a total of $count $label of this type.", count))
        return ShatterItemTooltipContext(includeDefaultCargoAndPrice = false, lines = lines)
    }

    private fun unitCargoSpace(): Float {
        val stocks: List<SubmarketWeaponStock> = record.submarketStocks
        for (stock in stocks) {
            val value = stock.unitCargoSpace
            if (validNumber(value) && value > 0f) {
                return value
            }
        }
        val reference = StockItemStacks.referenceUnitCargoSpace(record.itemType, record.itemId)
        if (validNumber(reference) && reference > 0f) {
            return reference
        }
        return Float.NaN
    }

    private fun formatOneDecimalTrim(value: Float): String {
        if (!validNumber(value)) {
            return "?"
        }
        val rounded = Math.round(value)
        if (Math.abs(value - rounded) < 0.05f) {
            return rounded.toString()
        }
        return String.format(Locale.US, "%.1f", value)
    }

    private fun hasText(value: String?): Boolean = value != null && value.trim().isNotEmpty()

    private fun validNumber(value: Float): Boolean = !value.isNaN() && !value.isInfinite()
}
