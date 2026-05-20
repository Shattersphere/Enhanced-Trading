package weaponsprocurement.autotrade

/**
 * Count-based threshold rule for a single weapon or fighter LPC.
 *
 * Semantics:
 *  - `sellAbove >= 0`: if player cargo count > sellAbove, sell the surplus down to sellAbove.
 *  - `buyBelow  >= 0`: if player cargo count < buyBelow, buy up to buyBelow (subject to
 *    availability and the credit floor).
 *  - A negative value means "unset / do nothing in this direction".
 *
 * Must stay a named class with a no-arg constructor and `@JvmField` fields so XStream can
 * round-trip it from a saved sector's persistent data without nulling out values.
 */
class AutoTradeItemRule() {
    @JvmField var sellAbove: Int = -1
    @JvmField var buyBelow: Int = -1

    constructor(sellAbove: Int, buyBelow: Int) : this() {
        this.sellAbove = sellAbove
        this.buyBelow = buyBelow
    }

    fun isEmpty(): Boolean = sellAbove < 0 && buyBelow < 0
}
