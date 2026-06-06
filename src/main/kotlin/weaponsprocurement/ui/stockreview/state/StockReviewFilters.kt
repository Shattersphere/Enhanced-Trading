package weaponsprocurement.ui.stockreview.state



import weaponsprocurement.stock.item.WeaponStockRecord
import java.util.EnumSet
import java.util.Locale

class StockReviewFilters private constructor() {
    companion object {
        @JvmStatic
        fun matches(record: WeaponStockRecord?, activeFilters: Set<StockReviewFilter>?): Boolean {
            if (activeFilters.isNullOrEmpty()) {
                return true
            }
            for (group in StockReviewFilterGroup.values()) {
                if (!matchesGroup(record, activeFilters, group)) {
                    return false
                }
            }
            return true
        }

        @JvmStatic
        fun count(activeFilters: Set<StockReviewFilter>?): Int = activeFilters?.size ?: 0

        @JvmStatic
        fun matchesSearch(record: WeaponStockRecord?, searchQuery: String?): Boolean =
            matchesSearchTerms(record, searchTerms(searchQuery))

        @JvmStatic
        fun matchesSearchTerms(record: WeaponStockRecord?, terms: List<String>): Boolean {
            if (terms.isEmpty()) {
                return true
            }
            if (record == null) {
                return false
            }
            val searchable = listOfNotNull(
                record.displayName,
                record.displayNameWithFixerMarker,
                record.itemId,
                record.itemKey,
                record.itemType.sectionLabel,
                record.itemType.singularLabel,
                record.sizeLabel,
                record.typeLabel,
                record.primaryRoleLabel,
                record.damageTypeLabel,
                record.fixerRarityLabel,
                record.fixerAvailabilityLabel,
            ).joinToString(" ").lowercase(Locale.ROOT)
            return terms.all { searchable.contains(it) }
        }

        @JvmStatic
        fun searchTerms(searchQuery: String?): List<String> =
            searchQuery
                ?.lowercase(Locale.ROOT)
                ?.split(Regex("\\s+"))
                ?.filter { it.isNotBlank() }
                ?: emptyList()

        @JvmStatic
        fun activeInGroup(activeFilters: Set<StockReviewFilter>?, group: StockReviewFilterGroup?): Set<StockReviewFilter> {
            val result = EnumSet.noneOf(StockReviewFilter::class.java)
            if (activeFilters == null || group == null) {
                return result
            }
            for (filter in activeFilters) {
                if (group == filter.group) {
                    result.add(filter)
                }
            }
            return result
        }

        private fun matchesGroup(
            record: WeaponStockRecord?,
            activeFilters: Set<StockReviewFilter>,
            group: StockReviewFilterGroup,
        ): Boolean {
            val groupFilters = activeInGroup(activeFilters, group)
            if (groupFilters.isEmpty()) {
                return true
            }
            if (group.weaponOnly && record != null && record.isWing()) {
                return true
            }
            for (filter in groupFilters) {
                if (filter.matches(record)) {
                    return true
                }
            }
            return false
        }
    }
}
