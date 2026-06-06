package weaponsprocurement.stock.fixer

internal object FixerReferenceValues {
    fun sanitizeBaseUnitPrice(baseUnitPrice: Int): Int = Math.max(0, baseUnitPrice)

    fun sanitizeUnitCargoSpace(unitCargoSpace: Float): Float {
        return if (isFiniteUnitCargoSpace(unitCargoSpace)) Math.max(0.01f, unitCargoSpace) else 1f
    }

    fun isFiniteUnitCargoSpace(unitCargoSpace: Float): Boolean {
        return !unitCargoSpace.isNaN() && !unitCargoSpace.isInfinite()
    }
}
