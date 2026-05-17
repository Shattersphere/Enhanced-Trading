package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.StockCategory
import weaponsprocurement.stock.item.StockItemType
import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewFilterGroup
import weaponsprocurement.ui.stockreview.tooltips.StockReviewTooltips
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeGroup
import java.awt.Color
import java.util.Locale

object StockReviewHeadingRows {
    private const val BASIC_INFO_SECTION = "basic"
    private const val ADVANCED_INFO_SECTION = "advanced"

    @JvmStatic
    fun itemType(itemType: StockItemType, count: Int, expanded: Boolean, topGap: Boolean): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.filterHeading(
            WimGuiToggleHeading.countedLabel(itemType.sectionLabel, count, expanded),
            StockReviewAction.toggle(itemType),
            topGap,
            showHideTooltip(itemType.sectionLabel),
        )

    @JvmStatic
    fun stockCategory(
        label: String,
        itemType: StockItemType,
        category: StockCategory,
        color: Color,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.categoryIndented(
            WimGuiToggleHeading.label(label, expanded),
            color,
            StockReviewAction.toggle(itemType, category),
            topGap,
            StockReviewTooltips.category(category),
            StockReviewStyle.WEAPON_INDENT,
        )

    @JvmStatic
    fun reviewGroup(
        tradeGroup: StockReviewTradeGroup,
        count: Int,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.category(
            WimGuiToggleHeading.countedLabel(tradeGroup.label, count, expanded),
            reviewGroupColor(tradeGroup),
            StockReviewAction.toggle(tradeGroup),
            topGap,
            "Show or hide queued ${tradeGroup.label.lowercase(Locale.US)} trades.",
        )

    @JvmStatic
    fun filterGroup(
        group: StockReviewFilterGroup,
        activeCount: Int,
        expanded: Boolean,
        topGap: Boolean,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.filterControlHeading(
            WimGuiToggleHeading.countedLabel(group.label, activeCount, expanded),
            StockReviewAction.toggle(group),
            topGap,
            StockReviewTooltips.filterHeading(group),
        )

    @JvmStatic
    fun basicInfo(record: WeaponStockRecord, expanded: Boolean, layout: StockReviewRowLayout): WimGuiListRow<StockReviewAction> =
        itemInfo("Basic Info", record, BASIC_INFO_SECTION, expanded, layout)

    @JvmStatic
    fun advancedInfo(record: WeaponStockRecord, expanded: Boolean, layout: StockReviewRowLayout): WimGuiListRow<StockReviewAction> =
        itemInfo("Advanced Info", record, ADVANCED_INFO_SECTION, expanded, layout)

    @JvmStatic
    fun itemLabel(record: WeaponStockRecord, expanded: Boolean): String =
        WimGuiToggleHeading.label(record.displayNameWithFixerMarker, expanded)

    @JvmStatic
    fun infoSectionKey(record: WeaponStockRecord, section: String): String = record.itemKey + "::info::" + section

    @JvmStatic
    fun basicInfoSectionKey(record: WeaponStockRecord): String = infoSectionKey(record, BASIC_INFO_SECTION)

    @JvmStatic
    fun advancedInfoSectionKey(record: WeaponStockRecord): String = infoSectionKey(record, ADVANCED_INFO_SECTION)

    private fun itemInfo(
        label: String,
        record: WeaponStockRecord,
        section: String,
        expanded: Boolean,
        layout: StockReviewRowLayout,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.nestedHeading(
            WimGuiToggleHeading.label(label, expanded),
            layout.infoIndent,
            layout.rightBlockWidth,
            StockReviewAction.toggleItem(infoSectionKey(record, section)),
            false,
            "Show or hide ${label.lowercase(Locale.US)} rows.",
        )

    private fun reviewGroupColor(tradeGroup: StockReviewTradeGroup): Color =
        if (StockReviewTradeGroup.BUYING == tradeGroup) StockReviewStyle.CONFIRM_BUTTON else StockReviewStyle.CANCEL_BUTTON

    private fun showHideTooltip(label: String): String = "Show or hide ${label.lowercase(Locale.US)}."
}
