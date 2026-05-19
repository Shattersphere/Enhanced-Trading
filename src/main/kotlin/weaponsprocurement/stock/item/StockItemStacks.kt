package weaponsprocurement.stock.item

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import kotlin.math.max

/**
 * Central price/cargo-space source for weapon and LPC stacks. Keep vanilla multipliers and
 * tariff math here so UI quotes, execution, summaries, and tooltips stay aligned.
 */
object StockItemStacks {
    private const val SETTING_WEAPON_BUY_MULT = "shipWeaponBuyPriceMult"
    private const val SETTING_WEAPON_SELL_MULT = "shipWeaponSellPriceMult"

    @JvmStatic
    fun isVisibleWeaponStack(stack: CargoStackAPI?): Boolean {
        return stack != null && stack.isWeaponStack && stack.weaponSpecIfWeapon != null && stack.size > 0f
    }

    @JvmStatic
    fun isVisibleWingStack(stack: CargoStackAPI?): Boolean {
        return stack != null && stack.isFighterWingStack && stack.fighterWingSpecIfWing != null && stack.size > 0f
    }

    @JvmStatic
    fun isVisibleItemStack(stack: CargoStackAPI?, itemType: StockItemType?): Boolean {
        return if (StockItemType.WING == itemType) {
            isVisibleWingStack(stack)
        } else {
            isVisibleWeaponStack(stack)
        }
    }

    @JvmStatic
    fun isPurchasableWeaponStack(submarket: SubmarketAPI?, stack: CargoStackAPI?): Boolean {
        if (submarket == null || stack == null || !stack.isWeaponStack || stack.weaponSpecIfWeapon == null) {
            return false
        }
        val plugin = submarket.plugin
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false
        }
        return stack.size > 0f
    }

    @JvmStatic
    fun isPurchasableWingStack(submarket: SubmarketAPI?, stack: CargoStackAPI?): Boolean {
        if (submarket == null || stack == null || !stack.isFighterWingStack || stack.fighterWingSpecIfWing == null) {
            return false
        }
        val plugin = submarket.plugin
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false
        }
        return stack.size > 0f
    }

    @JvmStatic
    fun isPurchasableItemStack(submarket: SubmarketAPI?, stack: CargoStackAPI?, itemType: StockItemType?): Boolean {
        return if (StockItemType.WING == itemType) {
            isPurchasableWingStack(submarket, stack)
        } else {
            isPurchasableWeaponStack(submarket, stack)
        }
    }

    @JvmStatic
    fun itemId(stack: CargoStackAPI?, itemType: StockItemType?): String? {
        if (stack == null) return null
        if (StockItemType.WING == itemType) {
            return stack.fighterWingSpecIfWing?.id
        }
        return stack.weaponSpecIfWeapon?.weaponId
    }

    @JvmStatic
    fun unitPrice(submarket: SubmarketAPI?, stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        val tariff = tariff(submarket)
        return max(0, Math.round(baseUnitPrice(stack) * (1f + max(0f, tariff))))
    }

    @JvmStatic
    fun baseUnitPrice(stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        return max(0, Math.round(stack.baseValuePerUnit.toFloat() * marketBuyMultiplier()))
    }

    @JvmStatic
    fun sellUnitPrice(submarket: SubmarketAPI?, stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        val tariff = tariff(submarket)
        return max(0, Math.round(sellBaseUnitPrice(stack) * (1f - max(0f, tariff))))
    }

    @JvmStatic
    fun sellBaseUnitPrice(stack: CargoStackAPI?): Int {
        if (stack == null) return 0
        return max(0, Math.round(stack.baseValuePerUnit.toFloat() * marketSellMultiplier()))
    }

    @JvmStatic
    fun buyTariffUnitPrice(submarket: SubmarketAPI?, stack: CargoStackAPI?): Int =
        max(0, unitPrice(submarket, stack) - baseUnitPrice(stack))

    @JvmStatic
    fun sellTariffUnitPrice(submarket: SubmarketAPI?, stack: CargoStackAPI?): Int =
        max(0, sellBaseUnitPrice(stack) - sellUnitPrice(submarket, stack))

    @JvmStatic
    fun unitCargoSpace(stack: CargoStackAPI?): Float {
        if (stack == null) return 1f
        val value = stack.cargoSpacePerUnit
        return if (value <= 0f) 1f else value
    }

    @JvmStatic
    fun referenceBaseUnitPrice(itemType: StockItemType?, itemId: String?): Int {
        return baseUnitPrice(referenceStack(itemType, itemId))
    }

    @JvmStatic
    fun referenceUnitCargoSpace(itemType: StockItemType?, itemId: String?): Float {
        return unitCargoSpace(referenceStack(itemType, itemId))
    }

    @JvmStatic
    fun referenceStack(itemType: StockItemType?, itemId: String?): CargoStackAPI? {
        if (itemType == null || itemId.isNullOrBlank()) return null
        return try {
            val cargoType = if (StockItemType.WING == itemType) {
                CargoAPI.CargoItemType.FIGHTER_CHIP
            } else {
                CargoAPI.CargoItemType.WEAPONS
            }
            Global.getSettings().createCargoStack(cargoType, itemId, null)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun tariff(submarket: SubmarketAPI?): Float {
        if (submarket == null) return 0f
        val plugin = submarket.plugin
        return plugin?.tariff ?: submarket.tariff
    }

    private fun marketBuyMultiplier(): Float = marketMultiplier(SETTING_WEAPON_BUY_MULT)

    private fun marketSellMultiplier(): Float = marketMultiplier(SETTING_WEAPON_SELL_MULT)

    private fun marketMultiplier(settingId: String): Float {
        return try {
            val settings = Global.getSettings() ?: return 1f
            max(0f, settings.getFloat(settingId))
        } catch (_: RuntimeException) {
            1f
        }
    }
}
