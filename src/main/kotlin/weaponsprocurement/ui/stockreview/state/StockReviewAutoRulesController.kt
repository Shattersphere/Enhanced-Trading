package weaponsprocurement.ui.stockreview.state

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import weaponsprocurement.autotrade.AutoTradeConfig
import weaponsprocurement.autotrade.AutoTradeItemRule
import weaponsprocurement.autotrade.AutoTradeRegistry
import weaponsprocurement.config.WeaponsProcurementConfig
import weaponsprocurement.stock.item.StockItemSpecs
import weaponsprocurement.stock.item.StockItemType

enum class AutoRulesTab(val label: String) {
    WEAPONS("Weapons"),
    FIGHTERS("Fighter LPCs"),
    HULLMODS("Hullmods");

    fun next(delta: Int): AutoRulesTab {
        val vs = values()
        val size = vs.size
        return vs[(((ordinal + delta) % size) + size) % size]
    }
}

enum class AutoRulesHeldFilter(val label: String) {
    ALL("Any held"),
    HELD("Held only"),
    NOT_HELD("Unheld only");

    fun next(delta: Int): AutoRulesHeldFilter {
        val vs = values()
        val size = vs.size
        return vs[(((ordinal + delta) % size) + size) % size]
    }
}

enum class AutoRulesRuleFilter(val label: String) {
    ALL("Any rule"),
    RULED("Ruled"),
    UNRULED("Unruled");

    fun next(delta: Int): AutoRulesRuleFilter {
        val vs = values()
        val size = vs.size
        return vs[(((ordinal + delta) % size) + size) % size]
    }
}

enum class AutoRulesNewFilter(val label: String) {
    ALL("Any age"),
    NEW("New only"),
    NOT_NEW("Seen only");

    fun next(delta: Int): AutoRulesNewFilter {
        val vs = values()
        val size = vs.size
        return vs[(((ordinal + delta) % size) + size) % size]
    }
}

enum class AutoRulesSizeFilter(val label: String) {
    ALL("Any size"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large");

    fun next(delta: Int): AutoRulesSizeFilter {
        val vs = values()
        val size = vs.size
        return vs[(((ordinal + delta) % size) + size) % size]
    }
}

enum class AutoRulesDamageFilter(val label: String) {
    ALL("Any mount"),
    ENERGY("Energy"),
    BALLISTIC("Ballistic"),
    MISSILE("Missile");

    fun next(delta: Int): AutoRulesDamageFilter {
        val vs = values()
        val size = vs.size
        return vs[(((ordinal + delta) % size) + size) % size]
    }
}

/**
 * Transient UI state for the AUTO_RULES screen. Persistent rules live in
 * [AutoTradeConfig]; this controller only tracks tab/filter, drives revision bumps for
 * re-render, and forwards mutations to the config.
 */
class StockReviewAutoRulesController {
    private var tab: AutoRulesTab = AutoRulesTab.WEAPONS
    private var heldFilter: AutoRulesHeldFilter = AutoRulesHeldFilter.ALL
    private var ruleFilter: AutoRulesRuleFilter = AutoRulesRuleFilter.ALL
    private var newFilter: AutoRulesNewFilter = AutoRulesNewFilter.ALL
    private var sizeFilter: AutoRulesSizeFilter = AutoRulesSizeFilter.ALL
    private var damageFilter: AutoRulesDamageFilter = AutoRulesDamageFilter.ALL
    private var designTypeFilter: String? = null
    private var designTypesCache: List<String>? = null
    private var nameQuery: String = ""
    private val selectedItemIds: MutableSet<String> = LinkedHashSet()
    private var bulkSellAbove: Int = -1
    private var bulkBuyBelow: Int = -1
    private var revision: Int = 0

    fun currentTab(): AutoRulesTab = tab
    fun currentHeldFilter(): AutoRulesHeldFilter = heldFilter
    fun currentRuleFilter(): AutoRulesRuleFilter = ruleFilter
    fun currentNewFilter(): AutoRulesNewFilter = newFilter
    fun currentSizeFilter(): AutoRulesSizeFilter = sizeFilter
    fun currentDamageFilter(): AutoRulesDamageFilter = damageFilter
    fun currentDesignTypeFilter(): String? = designTypeFilter
    fun currentDesignTypeLabel(): String = designTypeFilter ?: "Any design"
    fun currentNameQuery(): String = nameQuery
    fun currentBulkSellAbove(): Int = bulkSellAbove
    fun currentBulkBuyBelow(): Int = bulkBuyBelow
    fun isSelected(itemId: String): Boolean = selectedItemIds.contains(itemId)
    fun selectedCount(): Int = selectedItemIds.size
    fun getRevision(): Int = revision

    fun cycleTab(delta: Int) {
        val next = tab.next(if (delta == 0) 1 else delta)
        if (next != tab) {
            tab = next
            revision++
        }
    }

    fun cycleHeldFilter(delta: Int) {
        val next = heldFilter.next(if (delta == 0) 1 else delta)
        if (next != heldFilter) { heldFilter = next; revision++ }
    }

    fun cycleRuleFilter(delta: Int) {
        val next = ruleFilter.next(if (delta == 0) 1 else delta)
        if (next != ruleFilter) { ruleFilter = next; revision++ }
    }

    fun cycleNewFilter(delta: Int) {
        val next = newFilter.next(if (delta == 0) 1 else delta)
        if (next != newFilter) { newFilter = next; revision++ }
    }

    fun cycleSizeFilter(delta: Int) {
        val next = sizeFilter.next(if (delta == 0) 1 else delta)
        if (next != sizeFilter) { sizeFilter = next; revision++ }
    }

    fun cycleDamageFilter(delta: Int) {
        val next = damageFilter.next(if (delta == 0) 1 else delta)
        if (next != damageFilter) { damageFilter = next; revision++ }
    }

    fun cycleDesignTypeFilter(delta: Int) {
        val types = listDesignTypes()
        if (types.isEmpty()) return
        // Cycle order: null ("Any design") -> sorted distinct types -> back to null.
        val ordered: MutableList<String?> = ArrayList(types.size + 1)
        ordered.add(null)
        ordered.addAll(types)
        val currentIdx = ordered.indexOf(designTypeFilter).let { if (it < 0) 0 else it }
        val step = if (delta == 0) 1 else delta
        val size = ordered.size
        val nextIdx = (((currentIdx + step) % size) + size) % size
        val next = ordered[nextIdx]
        if (next != designTypeFilter) {
            designTypeFilter = next
            revision++
        }
    }

    private fun listDesignTypes(): List<String> {
        designTypesCache?.let { return it }
        val settings = Global.getSettings() ?: return emptyList()
        val seen = HashSet<String>()
        for (id in sellableUniverse(StockItemType.WEAPON)) {
            val m = settings.getWeaponSpec(id)?.manufacturer ?: continue
            if (m.isNotBlank()) seen.add(m)
        }
        val result = seen.sortedBy { it.lowercase() }
        designTypesCache = result
        return result
    }

    fun handleCommitNameQuery(value: String) {
        val trimmed = value.trim()
        if (trimmed != nameQuery) {
            nameQuery = trimmed
            revision++
        }
    }

    fun handleCommitBulkSellAbove(value: Int) {
        val clamped = if (value < 0) -1 else value.coerceIn(0, 999)
        if (clamped != bulkSellAbove) {
            bulkSellAbove = clamped
            revision++
        }
    }

    fun handleCommitBulkBuyBelow(value: Int) {
        val clamped = if (value < 0) -1 else value.coerceIn(0, 999)
        if (clamped != bulkBuyBelow) {
            bulkBuyBelow = clamped
            revision++
        }
    }

    fun handleToggleSelectItem(itemKey: String?) {
        if (itemKey.isNullOrEmpty()) return
        val id = StockItemType.rawId(itemKey) ?: itemKey
        if (!selectedItemIds.remove(id)) selectedItemIds.add(id)
        revision++
    }

    fun handleToggleSelectAllVisible() {
        val cfg = AutoTradeRegistry.get()
        val visible = visibleItemIds(cfg)
        if (visible.isEmpty()) return
        // If every visible row is already selected, deselect them; otherwise select all visible.
        val allSelected = visible.all { selectedItemIds.contains(it) }
        if (allSelected) {
            for (id in visible) selectedItemIds.remove(id)
        } else {
            for (id in visible) selectedItemIds.add(id)
        }
        revision++
    }

    fun handleApplyBulk() {
        if (selectedItemIds.isEmpty()) return
        if (bulkSellAbove < 0 && bulkBuyBelow < 0) return
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()
        visibleItemIds(cfg) // trims selections to visible set
        if (selectedItemIds.isEmpty()) return
        val map = ruleMap(cfg) ?: return
        for (id in selectedItemIds) {
            val rule = map.getOrPut(id) { AutoTradeItemRule() }
            if (bulkSellAbove >= 0) rule.sellAbove = bulkSellAbove
            if (bulkBuyBelow >= 0) rule.buyBelow = bulkBuyBelow
            if (rule.isEmpty()) map.remove(id)
        }
        revision++
    }

    fun handleClearSelected() {
        if (selectedItemIds.isEmpty()) return
        val cfg = AutoTradeRegistry.get()
        visibleItemIds(cfg) // trims selections to visible set
        if (selectedItemIds.isEmpty()) return
        val map = ruleMap(cfg) ?: return
        var changed = false
        for (id in selectedItemIds) {
            if (map.remove(id) != null) changed = true
        }
        if (changed) revision++
    }

    fun reset() {
        tab = AutoRulesTab.WEAPONS
        heldFilter = AutoRulesHeldFilter.ALL
        ruleFilter = AutoRulesRuleFilter.ALL
        newFilter = AutoRulesNewFilter.ALL
        sizeFilter = AutoRulesSizeFilter.ALL
        damageFilter = AutoRulesDamageFilter.ALL
        designTypeFilter = null
        nameQuery = ""
        selectedItemIds.clear()
        bulkSellAbove = -1
        bulkBuyBelow = -1
        revision++
    }

    fun handleToggleEnabled() = mutate { it.enabled = !it.enabled }
    fun handleToggleRequireConfirm() = mutate { it.requireConfirm = !it.requireConfirm }
    fun handleToggleSuspicionSelling() = mutate { it.allowSuspicionWhenSelling = !it.allowSuspicionWhenSelling }
    fun handleToggleSuspicionBuying() = mutate { it.allowSuspicionWhenBuying = !it.allowSuspicionWhenBuying }
    fun handleToggleBuyUnknownHullmods() = mutate { it.buyUnknownHullmods = !it.buyUnknownHullmods }
    fun handleToggleLearnHullmods() = mutate { it.learnHullmodsOnBuy = !it.learnHullmodsOnBuy }

    fun handleAdjustCreditFloor(delta: Int) = mutate {
        val next = (it.creditFloor.toLong() + delta).coerceIn(0L, 1_000_000_000L).toInt()
        it.creditFloor = next
    }

    fun handleCommitCreditFloor(value: Int) = mutate {
        val clamped = if (value < 0) 0 else value.coerceIn(0, 1_000_000_000)
        it.creditFloor = clamped
    }

    fun handleAdjustSellAbove(itemKey: String?, delta: Int) = mutateRule(itemKey) { r ->
        r.sellAbove = adjustThreshold(r.sellAbove, delta)
    }

    fun handleAdjustBuyBelow(itemKey: String?, delta: Int) = mutateRule(itemKey) { r ->
        r.buyBelow = adjustThreshold(r.buyBelow, delta)
    }

    fun handleCommitSellAbove(itemKey: String?, value: Int) = mutateRule(itemKey) { r ->
        r.sellAbove = if (value < 0) -1 else value.coerceIn(0, 999)
    }

    fun handleCommitBuyBelow(itemKey: String?, value: Int) = mutateRule(itemKey) { r ->
        r.buyBelow = if (value < 0) -1 else value.coerceIn(0, 999)
    }

    fun handleClearRule(itemKey: String?) {
        val cfg = AutoTradeRegistry.get()
        val (id, map) = resolveMap(cfg, itemKey) ?: return
        if (map.remove(id) != null) {
            revision++
        }
    }

    fun handleToggleHullmodBlacklist(hullmodId: String?) {
        if (hullmodId.isNullOrEmpty()) return
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()
        val set = cfg.hullmodBlacklist
        if (!set.remove(hullmodId)) set.add(hullmodId)
        revision++
    }

    private fun adjustThreshold(current: Int, delta: Int): Int {
        val base = if (current < 0) 0 else current
        return (base + delta).coerceIn(0, 999)
    }

    private fun mutate(block: (AutoTradeConfig) -> Unit) {
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()
        block(cfg)
        revision++
    }

    private fun mutateRule(itemKey: String?, block: (AutoTradeItemRule) -> Unit) {
        if (itemKey.isNullOrEmpty()) return
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()
        val (id, map) = resolveMap(cfg, itemKey) ?: return
        val rule = map.getOrPut(id) { AutoTradeItemRule() }
        block(rule)
        if (rule.isEmpty()) {
            map.remove(id)
        }
        revision++
    }

    private fun resolveMap(cfg: AutoTradeConfig, itemKey: String?): Pair<String, MutableMap<String, AutoTradeItemRule>>? {
        if (itemKey.isNullOrEmpty()) return null
        val itemType = StockItemType.fromKey(itemKey)
        val rawId = StockItemType.rawId(itemKey) ?: return null
        val map: MutableMap<String, AutoTradeItemRule> = if (itemType == StockItemType.WING) cfg.fighters else cfg.weapons
        return rawId to map
    }

    private fun ruleMap(cfg: AutoTradeConfig): MutableMap<String, AutoTradeItemRule>? = when (tab) {
        AutoRulesTab.WEAPONS -> cfg.weapons
        AutoRulesTab.FIGHTERS -> cfg.fighters
        AutoRulesTab.HULLMODS -> null // bulk operations are weapon/fighter only
    }

    /**
     * Build the set of item IDs that should appear in the current tab+filter view.
     * For the WEAPONS / FIGHTERS tabs, item IDs are raw weapon/wing ids; for HULLMODS,
     * they are hullmod ids.
     */
    fun visibleItemIds(cfg: AutoTradeConfig): List<String> {
        cfg.ensureSets()
        val ids = when (tab) {
            AutoRulesTab.WEAPONS -> collectItemRows(cfg, StockItemType.WEAPON)
            AutoRulesTab.FIGHTERS -> collectItemRows(cfg, StockItemType.WING)
            AutoRulesTab.HULLMODS -> collectHullmodRows()
        }
        // Drop selections for items no longer visible so a hidden row can't be silently
        // mutated by a later bulk apply.
        if (selectedItemIds.isNotEmpty()) {
            val visibleSet = ids.toHashSet()
            selectedItemIds.retainAll(visibleSet)
        }
        return ids
    }

    private fun collectItemRows(cfg: AutoTradeConfig, type: StockItemType): List<String> {
        val ruleMap = if (type == StockItemType.WING) cfg.fighters else cfg.weapons
        val owned = playerOwnedIds(type)
        val query = nameQuery.lowercase()

        val all = LinkedHashSet<String>()
        all.addAll(sellableUniverse(type))
        all.addAll(ruleMap.keys.filter { !it.isNullOrEmpty() })
        all.addAll(owned)

        val filtered = ArrayList<String>(all.size)
        for (id in all) {
            // Skip ids that do not resolve to a spec of this tab's type. This guards against
            // cross-typed entries from older bugs and stale ids from uninstalled mods, either
            // of which would otherwise throw "spec not found" while rendering the tab.
            val typeSpecExists = if (type == StockItemType.WING) {
                StockItemSpecs.wingSpec(id) != null
            } else {
                StockItemSpecs.weaponSpec(id) != null
            }
            if (!typeSpecExists) continue
            val isOwned = owned.contains(id)
            val rule = ruleMap[id]
            val isRuled = rule != null && !rule.isEmpty()
            val isNew = !isOwned && !isRuled

            // Held filter
            when (heldFilter) {
                AutoRulesHeldFilter.HELD -> if (!isOwned) continue
                AutoRulesHeldFilter.NOT_HELD -> if (isOwned) continue
                AutoRulesHeldFilter.ALL -> {}
            }
            // Rule filter
            when (ruleFilter) {
                AutoRulesRuleFilter.RULED -> if (!isRuled) continue
                AutoRulesRuleFilter.UNRULED -> if (isRuled) continue
                AutoRulesRuleFilter.ALL -> {}
            }
            // New filter
            when (newFilter) {
                AutoRulesNewFilter.NEW -> if (!isNew) continue
                AutoRulesNewFilter.NOT_NEW -> if (isNew) continue
                AutoRulesNewFilter.ALL -> {}
            }
            // Weapon-only filters: size + damage type
            if (type == StockItemType.WEAPON) {
                val spec = StockItemSpecs.weaponSpec(id)
                if (sizeFilter != AutoRulesSizeFilter.ALL) {
                    val s = spec?.size
                    val match = when (sizeFilter) {
                        AutoRulesSizeFilter.SMALL -> s == WeaponAPI.WeaponSize.SMALL
                        AutoRulesSizeFilter.MEDIUM -> s == WeaponAPI.WeaponSize.MEDIUM
                        AutoRulesSizeFilter.LARGE -> s == WeaponAPI.WeaponSize.LARGE
                        AutoRulesSizeFilter.ALL -> true
                    }
                    if (!match) continue
                }
                if (damageFilter != AutoRulesDamageFilter.ALL) {
                    val mount = spec?.type
                    val match = when (damageFilter) {
                        AutoRulesDamageFilter.ENERGY -> mount == WeaponAPI.WeaponType.ENERGY
                        AutoRulesDamageFilter.BALLISTIC -> mount == WeaponAPI.WeaponType.BALLISTIC
                        AutoRulesDamageFilter.MISSILE -> mount == WeaponAPI.WeaponType.MISSILE
                        AutoRulesDamageFilter.ALL -> true
                    }
                    if (!match) continue
                }
                val designSelection = designTypeFilter
                if (designSelection != null) {
                    if ((spec?.manufacturer ?: "") != designSelection) continue
                }
            }
            // Name query
            if (query.isNotEmpty()) {
                val name = displayName(type, id).lowercase()
                if (!name.contains(query)) continue
            }
            filtered.add(id)
        }
        return filtered.sortedBy { displayName(type, it).lowercase() }
    }

    /**
     * Universe of weapon / fighter IDs that any faction in the sector is known to sell.
     * Computed as the union of every faction's known-X set, filtered to drop entries
     * tagged with [Tags.NO_SELL] or its per-domain variants. Independent of the player's
     * market visits, so the editor lists everything purchasable anywhere in the sector.
     */
    private fun sellableUniverse(type: StockItemType): Set<String> {
        val sector = Global.getSector() ?: return emptySet()
        val settings = Global.getSettings() ?: return emptySet()
        val economy = sector.economy ?: return emptySet()
        // Restrict to factions that actually own at least one market in the sector.
        // This excludes hidden / lore-only factions (e.g. Threat, Shrouded Dwellers)
        // whose knownWeapons sets contain endgame-secret items the player can't buy.
        val marketFactionIds = HashSet<String>()
        for (market in economy.marketsCopy ?: emptyList()) {
            if (market == null) continue
            val fid = market.factionId ?: continue
            if (fid.isNotEmpty()) marketFactionIds.add(fid)
        }
        val out = LinkedHashSet<String>()
        for (fid in marketFactionIds) {
            val faction = sector.getFaction(fid) ?: continue
            val known: Collection<String>? = if (type == StockItemType.WING) faction.knownFighters else faction.knownWeapons
            if (known == null) continue
            for (id in known) {
                if (id.isNullOrEmpty()) continue
                val tags: Collection<String>? = if (type == StockItemType.WING) {
                    settings.getFighterWingSpec(id)?.tags
                } else {
                    settings.getWeaponSpec(id)?.tags
                }
                if (tags != null && hasNoSellTag(type, tags)) continue
                out.add(id)
            }
        }
        return out
    }

    private fun hasNoSellTag(type: StockItemType, tags: Collection<String>): Boolean {
        for (tag in tags) {
            if (tag == Tags.NO_SELL) return true
            if (type == StockItemType.WING && tag == Tags.WING_NO_SELL) return true
            if (type != StockItemType.WING && tag == Tags.WEAPON_NO_SELL) return true
        }
        return false
    }

    private fun collectHullmodRows(): List<String> {
        val all = ArrayList<HullModSpecAPI>(Global.getSettings()?.allHullModSpecs ?: emptyList())
        val character = Global.getSector()?.characterData
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()
        val query = nameQuery.lowercase()
        val visible = ArrayList<String>()
        for (hm in all) {
            if (hm.isHidden) continue
            val id = hm.id ?: continue
            val known = character?.knowsHullMod(id) == true
            val blacklisted = cfg.hullmodBlacklist.contains(id)
            val isNew = !known && !blacklisted

            // For hullmods, "held" = known.
            when (heldFilter) {
                AutoRulesHeldFilter.HELD -> if (!known) continue
                AutoRulesHeldFilter.NOT_HELD -> if (known) continue
                AutoRulesHeldFilter.ALL -> {}
            }
            // "Ruled" = blacklisted (the only rule kind for hullmods).
            when (ruleFilter) {
                AutoRulesRuleFilter.RULED -> if (!blacklisted) continue
                AutoRulesRuleFilter.UNRULED -> if (blacklisted) continue
                AutoRulesRuleFilter.ALL -> {}
            }
            when (newFilter) {
                AutoRulesNewFilter.NEW -> if (!isNew) continue
                AutoRulesNewFilter.NOT_NEW -> if (isNew) continue
                AutoRulesNewFilter.ALL -> {}
            }
            if (query.isNotEmpty()) {
                val name = (hm.displayName ?: id).lowercase()
                if (!name.contains(query)) continue
            }
            visible.add(id)
        }
        visible.sortBy { (Global.getSettings()?.getHullModSpec(it)?.displayName ?: it).lowercase() }
        return visible
    }

    private fun playerOwnedIds(type: StockItemType): Set<String> {
        val cargo = Global.getSector()?.playerFleet?.cargo ?: return emptySet()
        val out = LinkedHashSet<String>()
        val stacks = cargo.stacksCopy ?: return out
        for (stack in stacks) {
            if (type == StockItemType.WING) {
                if (stack.isFighterWingStack) stack.fighterWingSpecIfWing?.id?.let { out.add(it) }
            } else {
                if (stack.isWeaponStack) stack.weaponSpecIfWeapon?.weaponId?.let { out.add(it) }
            }
        }
        return out
    }

    private fun displayName(type: StockItemType, itemId: String): String {
        return if (type == StockItemType.WING) {
            StockItemSpecs.wingSpec(itemId)?.wingName ?: itemId
        } else {
            StockItemSpecs.weaponSpec(itemId)?.weaponName ?: itemId
        }
    }

    @Suppress("unused")
    private val unusedConfigLink: Class<WeaponsProcurementConfig> = WeaponsProcurementConfig::class.java
}
