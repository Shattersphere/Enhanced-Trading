package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import weaponsprocurement.autotrade.AutoTradeConfig
import weaponsprocurement.autotrade.AutoTradeRegistry
import weaponsprocurement.stock.item.StockItemSpecs
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionCells
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.AutoRulesDamageFilter
import weaponsprocurement.ui.stockreview.state.AutoRulesHeldFilter
import weaponsprocurement.ui.stockreview.state.AutoRulesNewFilter
import weaponsprocurement.ui.stockreview.state.AutoRulesRuleFilter
import weaponsprocurement.ui.stockreview.state.AutoRulesSizeFilter
import weaponsprocurement.ui.stockreview.state.AutoRulesTab
import weaponsprocurement.ui.stockreview.state.StockReviewAutoRulesController
import java.awt.Color

object StockReviewAutoRulesRows {

    private const val WIDE_VALUE_WIDTH = 320f
    private const val NAME_WIDTH = 300f
    private const val COUNT_WIDTH = 60f
    private const val THRESHOLD_LABEL_WIDTH = 60f
    private const val THRESHOLD_VALUE_WIDTH = 50f
    private const val SELECT_WIDTH = 40f
    private const val STEP_WIDTH = StockReviewStyle.TRADE_STEP_BUTTON_WIDTH
    private const val CLEAR_WIDTH = 60f

    @JvmStatic
    fun build(
        rowLayout: StockReviewRowLayout,
        controller: StockReviewAutoRulesController?,
    ): List<WimGuiListRow<StockReviewAction>> {
        val left = buildLeftRows(rowLayout, controller)
        val right = buildRightRows(rowLayout, controller)
        if (right.isEmpty()) return left
        if (left.isEmpty()) return right
        val combined = ArrayList<WimGuiListRow<StockReviewAction>>(left.size + right.size)
        combined.addAll(left)
        combined.addAll(right)
        return combined
    }

    /** Left pane: global toggles, credit floor, category tabs, filter chips, and bulk-apply row. */
    @JvmStatic
    fun buildLeftRows(
        @Suppress("UNUSED_PARAMETER") rowLayout: StockReviewRowLayout,
        controller: StockReviewAutoRulesController?,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()

        // Global toggles
        rows.add(toggleRow("Auto-trade enabled", cfg.enabled, StockReviewAction.autoRulesToggleEnabled(), "Master switch for automatic buy/sell when opening a market."))
        rows.add(toggleRow("Allow suspicion when selling", cfg.allowSuspicionWhenSelling, StockReviewAction.autoRulesToggleSuspicionSelling(), "Allow auto-sells to use the black market while your transponder is on, raising smuggling suspicion. With your transponder off you are anonymous, so the black market is always used regardless of this toggle."))
        rows.add(toggleRow("Allow suspicion when buying", cfg.allowSuspicionWhenBuying, StockReviewAction.autoRulesToggleSuspicionBuying(), "Allow auto-buys (weapons, fighter LPCs, and hullmods) to use the black market while your transponder is on, raising smuggling suspicion. With your transponder off the black market is the only market, so buys there happen regardless."))
        rows.add(toggleRow("Buy unknown hullmods", cfg.buyUnknownHullmods, StockReviewAction.autoRulesToggleBuyUnknownHullmods(), "Automatically purchase modspecs your character does not already know."))
        rows.add(toggleRow("Learn hullmods on purchase", cfg.learnHullmodsOnBuy, StockReviewAction.autoRulesToggleLearnHullmods(), "If on, purchased hullmods are added to your known list immediately. If off, the modspec is added to cargo as a learnable item."))

        // Credit floor
        rows.add(creditFloorRow(cfg.creditFloor, controller))

        if (controller == null) return rows

        // Tab + filter chip cyclers
        rows.add(tabRow(controller.currentTab()))
        rows.add(heldFilterRow(controller.currentHeldFilter()))
        rows.add(ruleFilterRow(controller.currentRuleFilter()))
        rows.add(newFilterRow(controller.currentNewFilter()))
        if (controller.currentTab() == AutoRulesTab.WEAPONS) {
            rows.add(sizeFilterRow(controller.currentSizeFilter()))
            rows.add(damageFilterRow(controller.currentDamageFilter()))
            rows.add(designTypeFilterRow(controller.currentDesignTypeLabel()))
        }
        rows.add(nameSearchRow(controller))

        // Bulk controls (only for weapon/fighter tabs)
        if (controller.currentTab() != AutoRulesTab.HULLMODS) {
            rows.add(bulkActionRow(controller))
            rows.add(bulkValuesRow(controller))
        }
        return rows
    }

    /** Right pane: the per-item rows for the current tab (or the empty-state info row). */
    @JvmStatic
    fun buildRightRows(
        @Suppress("UNUSED_PARAMETER") rowLayout: StockReviewRowLayout,
        controller: StockReviewAutoRulesController?,
    ): List<WimGuiListRow<StockReviewAction>> {
        val rows = ArrayList<WimGuiListRow<StockReviewAction>>()
        val cfg = AutoTradeRegistry.get()
        cfg.ensureSets()

        if (controller == null) {
            rows.add(infoOnlyRow("Auto-rules controller not initialized."))
            return rows
        }

        val itemIds = controller.visibleItemIds(cfg)
        if (itemIds.isEmpty()) {
            rows.add(infoOnlyRow("No items match the current tab and filter."))
        } else {
            when (controller.currentTab()) {
                AutoRulesTab.WEAPONS -> {
                    for (id in itemIds) {
                        val spec = StockItemSpecs.weaponSpec(id) ?: continue
                        rows.add(weaponRow(id, spec, cfg, controller))
                    }
                }
                AutoRulesTab.FIGHTERS -> {
                    for (id in itemIds) {
                        val spec = StockItemSpecs.wingSpec(id) ?: continue
                        rows.add(fighterRow(id, spec, cfg, controller))
                    }
                }
                AutoRulesTab.HULLMODS -> {
                    for (id in itemIds) {
                        rows.add(hullmodRow(id, cfg))
                    }
                }
            }
        }
        return rows
    }

    private fun toggleRow(label: String, value: Boolean, action: StockReviewAction, tooltip: String): WimGuiListRow<StockReviewAction> {
        val (text, fill) = if (value) "On" to StockReviewStyle.CONFIRM_BUTTON else "Off" to StockReviewStyle.CANCEL_BUTTON
        return formRow(
            label,
            WimGuiRowCell.of(
                actionCell(text, WIDE_VALUE_WIDTH, fill, action, tooltip),
            ),
        )
    }

    private fun creditFloorRow(value: Int, controller: StockReviewAutoRulesController?): WimGuiListRow<StockReviewAction> {
        return formRow(
            "Credit floor",
            WimGuiRowCell.of(
                textFieldCell(
                    key = "autoRules:creditFloor",
                    width = 160f,
                    initialText = value.toString(),
                    blurText = "0",
                    tooltip = "Auto-trade will not spend below this number of credits. Type a value and click away to commit.",
                ) { committed ->
                    val clamped = if (committed < 0) 0 else committed
                    controller?.handleCommitCreditFloor(clamped)
                },
            ),
        )
    }

    private fun tabRow(current: AutoRulesTab): WimGuiListRow<StockReviewAction> {
        val cells = AutoRulesTab.values().map { tab ->
            val fill = if (tab == current) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
            val delta = tab.ordinal - current.ordinal
            actionCell(tab.label, 130f, fill, StockReviewAction.autoRulesCycleTab(if (delta == 0) 1 else delta), "Switch to the ${tab.label} tab.")
        }
        return formRow("Category", WimGuiRowCell.of(*cells.toTypedArray()))
    }

    private fun heldFilterRow(current: AutoRulesHeldFilter): WimGuiListRow<StockReviewAction> {
        val cells = AutoRulesHeldFilter.values().map { f ->
            val fill = if (f == current) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
            val delta = f.ordinal - current.ordinal
            actionCell(f.label, 110f, fill, StockReviewAction.autoRulesCycleHeldFilter(if (delta == 0) 1 else delta), "Filter by whether the item is in your cargo.")
        }
        return formRow("Held", WimGuiRowCell.of(*cells.toTypedArray()))
    }

    private fun ruleFilterRow(current: AutoRulesRuleFilter): WimGuiListRow<StockReviewAction> {
        val cells = AutoRulesRuleFilter.values().map { f ->
            val fill = if (f == current) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
            val delta = f.ordinal - current.ordinal
            actionCell(f.label, 110f, fill, StockReviewAction.autoRulesCycleRuleFilter(if (delta == 0) 1 else delta), "Filter by whether the item has an auto-trade rule.")
        }
        return formRow("Rule", WimGuiRowCell.of(*cells.toTypedArray()))
    }

    private fun newFilterRow(current: AutoRulesNewFilter): WimGuiListRow<StockReviewAction> {
        val cells = AutoRulesNewFilter.values().map { f ->
            val fill = if (f == current) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
            val delta = f.ordinal - current.ordinal
            actionCell(f.label, 110f, fill, StockReviewAction.autoRulesCycleNewFilter(if (delta == 0) 1 else delta), "New = never held and no rule yet.")
        }
        return formRow("New", WimGuiRowCell.of(*cells.toTypedArray()))
    }

    private fun sizeFilterRow(current: AutoRulesSizeFilter): WimGuiListRow<StockReviewAction> {
        val cells = AutoRulesSizeFilter.values().map { f ->
            val fill = if (f == current) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
            val delta = f.ordinal - current.ordinal
            actionCell(f.label, 110f, fill, StockReviewAction.autoRulesCycleSizeFilter(if (delta == 0) 1 else delta), "Filter weapons by mount size.")
        }
        return formRow("Size", WimGuiRowCell.of(*cells.toTypedArray()))
    }

    private fun damageFilterRow(current: AutoRulesDamageFilter): WimGuiListRow<StockReviewAction> {
        val cells = AutoRulesDamageFilter.values().map { f ->
            val fill = if (f == current) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
            val delta = f.ordinal - current.ordinal
            actionCell(f.label, 110f, fill, StockReviewAction.autoRulesCycleDamageFilter(if (delta == 0) 1 else delta), "Filter weapons by mount type.")
        }
        return formRow("Mount", WimGuiRowCell.of(*cells.toTypedArray()))
    }

    private fun nameSearchRow(controller: StockReviewAutoRulesController): WimGuiListRow<StockReviewAction> {
        return formRow(
            "Name",
            WimGuiRowCell.of(
                WimGuiRowCell.textFieldString<StockReviewAction>(
                    key = "autoRules:nameSearch",
                    width = 320f,
                    initialText = controller.currentNameQuery(),
                    blurText = "(any)",
                    fillColor = StockReviewStyle.CELL_BACKGROUND,
                    textColor = weaponsprocurement.ui.WimGuiStyle.DEFAULT_TEXT,
                    maxChars = 48,
                    live = true,
                    onCommit = { committed -> controller.handleCommitNameQuery(committed) },
                    tooltip = "Substring filter on item name. The list rebuilds as you type.",
                ),
            ),
        )
    }

    private fun designTypeFilterRow(currentLabel: String): WimGuiListRow<StockReviewAction> {
        return formRow(
            "Design",
            WimGuiRowCell.of(
                actionCell("<", 30f, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.autoRulesCycleDesignTypeFilter(-1), "Previous design type."),
                infoCell(currentLabel, 220f, StockReviewStyle.CELL_BACKGROUND, "Current design type filter. \"Any design\" matches all weapons."),
                actionCell(">", 30f, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.autoRulesCycleDesignTypeFilter(1), "Next design type."),
            ),
        )
    }

    private fun bulkActionRow(controller: StockReviewAutoRulesController): WimGuiListRow<StockReviewAction> {
        val selected = controller.selectedCount()
        return formRow(
            "Bulk ($selected selected)",
            WimGuiRowCell.of(
                actionCell("Select all visible", 150f, StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.autoRulesToggleSelectAllVisible(), "Toggle selection for every visible row."),
                actionCell("Apply to selected", 150f, StockReviewStyle.CONFIRM_BUTTON, StockReviewAction.autoRulesApplyBulk(), "Write the non-blank bulk values to every selected item."),
                actionCell("Clear selected", 130f, StockReviewStyle.CANCEL_BUTTON, StockReviewAction.autoRulesClearSelected(), "Remove rules for every selected item."),
            ),
        )
    }

    private fun bulkValuesRow(controller: StockReviewAutoRulesController): WimGuiListRow<StockReviewAction> {
        val sellInit = if (controller.currentBulkSellAbove() < 0) "" else controller.currentBulkSellAbove().toString()
        val buyInit = if (controller.currentBulkBuyBelow() < 0) "" else controller.currentBulkBuyBelow().toString()
        return formRow(
            "Bulk values",
            WimGuiRowCell.of(
                infoCell("Sell>", THRESHOLD_LABEL_WIDTH, StockReviewStyle.CELL_BACKGROUND, null),
                textFieldCell(
                    key = "autoRules:bulkSellAbove",
                    width = THRESHOLD_VALUE_WIDTH,
                    initialText = sellInit,
                    blurText = "-",
                    tooltip = "Bulk sell-above. Blank = leave each selected item's sell-above alone.",
                ) { committed -> controller.handleCommitBulkSellAbove(committed) },
                infoCell("Buy<", THRESHOLD_LABEL_WIDTH, StockReviewStyle.CELL_BACKGROUND, null),
                textFieldCell(
                    key = "autoRules:bulkBuyBelow",
                    width = THRESHOLD_VALUE_WIDTH,
                    initialText = buyInit,
                    blurText = "-",
                    tooltip = "Bulk buy-below. Blank = leave each selected item's buy-below alone.",
                ) { committed -> controller.handleCommitBulkBuyBelow(committed) },
            ),
        )
    }

    private fun weaponRow(id: String, spec: WeaponSpecAPI, cfg: AutoTradeConfig, controller: StockReviewAutoRulesController): WimGuiListRow<StockReviewAction> {
        val owned = playerWeaponCount(id)
        val rule = cfg.weapons[id]
        return ruleRow(spec.weaponName ?: id, owned, rule?.sellAbove ?: -1, rule?.buyBelow ?: -1, id, "weapon", controller)
    }

    private fun fighterRow(id: String, spec: FighterWingSpecAPI, cfg: AutoTradeConfig, controller: StockReviewAutoRulesController): WimGuiListRow<StockReviewAction> {
        val owned = playerWingCount(id)
        val rule = cfg.fighters[id]
        return ruleRow(spec.wingName ?: id, owned, rule?.sellAbove ?: -1, rule?.buyBelow ?: -1, id, "fighter", controller)
    }

    private fun ruleRow(
        name: String,
        owned: Int,
        sellAbove: Int,
        buyBelow: Int,
        rawId: String,
        scope: String,
        controller: StockReviewAutoRulesController,
    ): WimGuiListRow<StockReviewAction> {
        val sellInitial = if (sellAbove < 0) "-" else sellAbove.toString()
        val buyInitial = if (buyBelow < 0) "-" else buyBelow.toString()
        val selected = controller.isSelected(rawId)
        val checkLabel = if (selected) "[x]" else "[ ]"
        val checkFill = if (selected) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.ACTION_BACKGROUND
        val itemKey = if (scope == "fighter") StockItemType.WING.key(rawId) else StockItemType.WEAPON.key(rawId)
        return formRow(
            null,
            WimGuiRowCell.of(
                actionCell(checkLabel, SELECT_WIDTH, checkFill, StockReviewAction.autoRulesToggleSelectItem(itemKey), "Toggle selection for bulk apply."),
                infoCell(name, NAME_WIDTH, StockReviewStyle.CELL_BACKGROUND, "Current rule for $name."),
                infoCell(owned.toString(), COUNT_WIDTH, StockReviewStyle.CELL_BACKGROUND, "Number currently in your cargo."),
                infoCell("Sell>", THRESHOLD_LABEL_WIDTH, StockReviewStyle.CELL_BACKGROUND, null),
                textFieldCell(
                    key = "autoRules:sellAbove:$scope:$rawId",
                    width = THRESHOLD_VALUE_WIDTH,
                    initialText = sellInitial,
                    blurText = "-",
                    tooltip = "Sell whenever you own more than this many. Blank = no rule.",
                ) { committed -> controller.handleCommitSellAbove(itemKey, committed) },
                infoCell("Buy<", THRESHOLD_LABEL_WIDTH, StockReviewStyle.CELL_BACKGROUND, null),
                textFieldCell(
                    key = "autoRules:buyBelow:$scope:$rawId",
                    width = THRESHOLD_VALUE_WIDTH,
                    initialText = buyInitial,
                    blurText = "-",
                    tooltip = "Buy whenever you own fewer than this many. Blank = no rule.",
                ) { committed -> controller.handleCommitBuyBelow(itemKey, committed) },
                actionCell("Clear", CLEAR_WIDTH, StockReviewStyle.CANCEL_BUTTON, StockReviewAction.autoRulesClearRule(itemKey), "Remove the rule for this item."),
            ),
        )
    }

    private fun hullmodRow(id: String, cfg: AutoTradeConfig): WimGuiListRow<StockReviewAction> {
        val spec = Global.getSettings()?.getHullModSpec(id)
        val name = spec?.displayName ?: id
        val knows = Global.getSector()?.characterData?.knowsHullMod(id) == true
        val blacklisted = cfg.hullmodBlacklist.contains(id)
        val statusLabel = when {
            blacklisted -> "Blacklisted"
            knows -> "Known"
            else -> "Unknown"
        }
        val toggleLabel = if (blacklisted) "Allow" else "Blacklist"
        val toggleFill = if (blacklisted) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.CANCEL_BUTTON
        return formRow(
            null,
            WimGuiRowCell.of(
                infoCell(name, NAME_WIDTH, StockReviewStyle.CELL_BACKGROUND, "Hullmod: $name"),
                infoCell(statusLabel, 130f, StockReviewStyle.CELL_BACKGROUND, "Current status for $name."),
                actionCell(toggleLabel, 140f, toggleFill, StockReviewAction.autoRulesToggleHullmodBlacklist(id), "Toggle whether auto-trade may purchase this hullmod."),
            ),
        )
    }

    private fun formRow(label: String?, cells: List<WimGuiRowCell<StockReviewAction>>): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(StockReviewRowSpecs.form(label, cells))

    private fun infoOnlyRow(label: String): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(StockReviewRowSpecs.empty(label))

    private fun actionCell(
        label: String,
        width: Float,
        fill: Color,
        action: StockReviewAction,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> =
        StockReviewActionCells.standard(StockReviewActionGroup.AUTO_RULES, label, width, fill, action, true, tooltip)

    private fun infoCell(
        label: String?,
        width: Float,
        fill: Color,
        tooltip: String?,
    ): WimGuiRowCell<StockReviewAction> =
        StockReviewCellGroup.infoCell(label, width, fill, tooltip, com.fs.starfarer.api.ui.Alignment.MID)

    private fun textFieldCell(
        key: String,
        width: Float,
        initialText: String,
        blurText: String,
        tooltip: String?,
        onCommit: (Int) -> Unit,
    ): WimGuiRowCell<StockReviewAction> = WimGuiRowCell.textField(
        key,
        width,
        initialText,
        blurText,
        StockReviewStyle.CELL_BACKGROUND,
        weaponsprocurement.ui.WimGuiStyle.DEFAULT_TEXT,
        onCommit,
        tooltip,
    )

    private fun playerWeaponCount(weaponId: String): Int {
        val cargo = Global.getSector()?.playerFleet?.cargo ?: return 0
        var total = 0
        for (stack in cargo.stacksCopy) {
            if (stack.isWeaponStack && stack.weaponSpecIfWeapon?.weaponId == weaponId) {
                total += stack.size.toInt()
            }
        }
        return total
    }

    private fun playerWingCount(wingId: String): Int {
        val cargo = Global.getSector()?.playerFleet?.cargo ?: return 0
        var total = 0
        for (stack in cargo.stacksCopy) {
            if (stack.isFighterWingStack && stack.fighterWingSpecIfWing?.id == wingId) {
                total += stack.size.toInt()
            }
        }
        return total
    }

    private fun formatCredits(value: Int): String {
        return "%,d cr".format(value)
    }
}
