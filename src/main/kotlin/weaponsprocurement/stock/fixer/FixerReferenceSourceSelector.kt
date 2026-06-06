package weaponsprocurement.stock.fixer

import weaponsprocurement.stock.item.SubmarketWeaponStock

internal object FixerReferenceSourceSelector {
    fun cheapest(sources: List<SubmarketWeaponStock>?): SubmarketWeaponStock? {
        if (sources == null) return null
        var best: SubmarketWeaponStock? = null
        for (source in sources) {
            if (!isReferenceSource(source)) continue
            val currentBest = best
            if (currentBest == null || compare(source, currentBest) < 0) {
                best = source
            }
        }
        return best
    }

    fun compare(left: SubmarketWeaponStock, right: SubmarketWeaponStock): Int {
        val result = left.baseUnitPrice.compareTo(right.baseUnitPrice)
        return if (result != 0) {
            result
        } else {
            left.displaySourceName.orEmpty().compareTo(right.displaySourceName.orEmpty(), ignoreCase = true)
        }
    }

    private fun isReferenceSource(source: SubmarketWeaponStock): Boolean {
        return source.count > 0 && source.isPurchasable()
    }
}
