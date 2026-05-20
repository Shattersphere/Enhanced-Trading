package weaponsprocurement.autotrade

import com.fs.starfarer.api.campaign.CargoAPI

/**
 * Root persistent config object for the auto-trade subsystem.
 *
 * Stored in `sector.persistentData` under [AutoTradeRegistry.KEY]. Must stay a named class
 * with a no-arg constructor and `@JvmField`-backed fields so XStream can deserialize it
 * cleanly from an existing save.
 *
 * Defaults for the boolean toggles are populated from LunaSettings the first time
 * [AutoTradeRegistry.get] runs against a sector that does not yet have a persisted
 * config; after that, persistent data wins.
 */
class AutoTradeConfig {

    /** Per-weapon threshold rules, keyed by weapon spec id. */
    @JvmField var weapons: MutableMap<String, AutoTradeItemRule> = LinkedHashMap()

    /** Per-fighter-LPC threshold rules, keyed by wing spec id. */
    @JvmField var fighters: MutableMap<String, AutoTradeItemRule> = LinkedHashMap()

    /** Master switch. When false, no automatic trades happen on market open. */
    @JvmField var enabled: Boolean = true

    /** Route auto-sells through the black market in addition to the open market. */
    @JvmField var sellThroughBlack: Boolean = false

    /** Route auto-buys (weapons and fighter LPCs) through the black market in addition to the open market. */
    @JvmField var buyThroughBlack: Boolean = false

    /**
     * Deprecated: black-market routing is now automatic whenever the transponder is off.
     * Kept as a `@JvmField` purely so existing save files deserialize without an XStream
     * UnknownField error; the value is never read.
     */
    @Deprecated("Black market is always used when the transponder is off; this field is ignored.")
    @JvmField var useBlackWhenTransponderOff: Boolean = false

    /** Additionally scan the black market for unknown hullmods to auto-buy. */
    @JvmField var buyHullmodsFromBlack: Boolean = false

    /** Auto-buy hullmods the player faction has not yet learned. */
    @JvmField var buyUnknownHullmods: Boolean = false

    /** After auto-buying a hullmod, also add it to the player faction's known list. */
    @JvmField var learnHullmodsOnBuy: Boolean = false

    /** Hullmod ids to never auto-buy (overrides [buyUnknownHullmods]). */
    @JvmField var hullmodBlacklist: MutableSet<String> = LinkedHashSet()

    /** Never spend credits below this floor on auto-buys. */
    @JvmField var creditFloor: Int = 0

    /** Weapon ids the player has ever held. Backs the "New" filter in the rules editor. */
    @JvmField var seenWeapons: MutableSet<String> = LinkedHashSet()

    /** Fighter wing ids the player has ever held. Backs the "New" filter. */
    @JvmField var seenFighters: MutableSet<String> = LinkedHashSet()

    /** Weapon ids the player has ever set a rule for. Cleared rules stay "not new". */
    @JvmField var ruledWeapons: MutableSet<String> = LinkedHashSet()

    /** Fighter wing ids the player has ever set a rule for. */
    @JvmField var ruledFighters: MutableSet<String> = LinkedHashSet()

    /**
     * Record everything currently in the cargo as "seen". Safe to call repeatedly.
     * Called once per market open before the engine runs.
     */
    fun markSeenFromCargo(cargo: CargoAPI?) {
        ensureSets()
        if (cargo == null) return
        cargo.weapons?.forEach { q ->
            val id = q?.item ?: return@forEach
            if (q.count > 0) seenWeapons.add(id)
        }
        cargo.fighters?.forEach { q ->
            val id = q?.item ?: return@forEach
            if (q.count > 0) seenFighters.add(id)
        }
    }

    /**
     * Record everything stocked at a submarket as "seen", so the auto-rules editor
     * surfaces items the player can see for sale even when they don't yet own any.
     */
    fun markSeenFromSubmarket(cargo: CargoAPI?) {
        markSeenFromCargo(cargo)
    }

    /**
     * Initialize any null collections. Called from [AutoTradeRegistry.get] so configs
     * loaded from older saves get new fields populated without crashing.
     */
    fun ensureSets() {
        @Suppress("SENSELESS_COMPARISON")
        if (weapons == null) weapons = LinkedHashMap()
        @Suppress("SENSELESS_COMPARISON")
        if (fighters == null) fighters = LinkedHashMap()
        @Suppress("SENSELESS_COMPARISON")
        if (hullmodBlacklist == null) hullmodBlacklist = LinkedHashSet()
        @Suppress("SENSELESS_COMPARISON")
        if (seenWeapons == null) seenWeapons = LinkedHashSet()
        @Suppress("SENSELESS_COMPARISON")
        if (seenFighters == null) seenFighters = LinkedHashSet()
        @Suppress("SENSELESS_COMPARISON")
        if (ruledWeapons == null) ruledWeapons = LinkedHashSet()
        @Suppress("SENSELESS_COMPARISON")
        if (ruledFighters == null) ruledFighters = LinkedHashSet()
    }
}
