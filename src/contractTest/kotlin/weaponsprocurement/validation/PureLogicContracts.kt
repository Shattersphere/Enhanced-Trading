package weaponsprocurement.validation

import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.StockSortMode
import weaponsprocurement.trade.plan.TradeMoney

private fun expect(label: String, actual: Any?, expected: Any?) {
    check(actual == expected) { "$label expected <$expected> but was <$actual>" }
}

private fun expectTrue(label: String, value: Boolean) {
    check(value) { "$label expected true" }
}

private fun expectFalse(label: String, value: Boolean) {
    check(!value) { "$label expected false" }
}

fun main() {
    verifyItemKeys()
    verifySortAliases()
    verifyTradeMoney()
    println("Pure logic contract validation passed.")
}

private fun verifyItemKeys() {
    expect("weapon key", StockItemType.WEAPON.key("lightmg"), "W:lightmg")
    expect("wing key", StockItemType.WING.key("broadsword_wing"), "F:broadsword_wing")
    expect("weapon null key", StockItemType.WEAPON.key(null), "W:")
    expect("wing null key", StockItemType.WING.key(null), "F:")
    expect("null key defaults to weapon", StockItemType.fromKey(null), StockItemType.WEAPON)
    expect("raw weapon id", StockItemType.rawId("W:lightmg"), "lightmg")
    expect("raw wing id", StockItemType.rawId("F:broadsword_wing"), "broadsword_wing")
    expect("raw legacy id", StockItemType.rawId("lightmg"), "lightmg")
    expect("raw null id", StockItemType.rawId(null), null)
    expect("wing typed key", StockItemType.fromKey("F:broadsword_wing"), StockItemType.WING)
    expect("weapon typed key", StockItemType.fromKey("W:lightmg"), StockItemType.WEAPON)
    expect("legacy id defaults to weapon", StockItemType.fromKey("lightmg"), StockItemType.WEAPON)
}

private fun verifySortAliases() {
    expect("null sort", StockSortMode.fromConfig(null), StockSortMode.NEED)
    expect("blank sort", StockSortMode.fromConfig(" "), StockSortMode.NEED)
    expect("name sort", StockSortMode.fromConfig("name"), StockSortMode.NAME)
    expect("rarity sort", StockSortMode.fromConfig("rarity"), StockSortMode.RARITY)
    expect("cost alias", StockSortMode.fromConfig("cost"), StockSortMode.PRICE)
    expect("hyphen alias", StockSortMode.fromConfig("for-sale"), StockSortMode.NEED)
    expect("space alias", StockSortMode.fromConfig("for sale"), StockSortMode.NEED)
    expect("purchasable alias", StockSortMode.fromConfig("PURCHASABLE"), StockSortMode.NEED)
    expect("owned alias", StockSortMode.fromConfig("OWNED"), StockSortMode.NEED)
    expect("unknown sort", StockSortMode.fromConfig("not-a-sort"), StockSortMode.NEED)
}

private fun verifyTradeMoney() {
    expect("line total", TradeMoney.lineTotal(12, 5), 60L)
    expect("negative unit price", TradeMoney.lineTotal(-1, 5), -1L)
    expect("negative quantity", TradeMoney.lineTotal(12, -1), -1L)
    expect("safe add", TradeMoney.safeAdd(7L, 5L), 12L)
    expect("safe add positive saturation", TradeMoney.safeAdd(Long.MAX_VALUE, 1L), Long.MAX_VALUE)
    expect("safe add negative saturation", TradeMoney.safeAdd(Long.MIN_VALUE, -1L), Long.MIN_VALUE)
    expectTrue("zero credit mutation", TradeMoney.canExecuteCreditMutation(0L))
    expectTrue("max credit mutation", TradeMoney.canExecuteCreditMutation(TradeMoney.MAX_EXECUTABLE_CREDITS))
    expectFalse("negative credit mutation", TradeMoney.canExecuteCreditMutation(-1L))
    expectFalse("too large credit mutation", TradeMoney.canExecuteCreditMutation(TradeMoney.MAX_EXECUTABLE_CREDITS + 1L))
}
