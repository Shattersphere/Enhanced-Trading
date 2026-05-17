package weaponsprocurement.ui.stockreview.rows

import weaponsprocurement.stock.item.WeaponStockRecord
import weaponsprocurement.ui.WimGuiListRow
import weaponsprocurement.ui.WimGuiToggleHeading
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import java.util.Locale

object StockReviewItemDetailHeadingRows {
    private const val BASIC_INFO_SECTION = "basic"
    private const val ADVANCED_INFO_SECTION = "advanced"

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
    fun basicInfoSectionKey(record: WeaponStockRecord): String = infoSectionKey(record, BASIC_INFO_SECTION)

    @JvmStatic
    fun advancedInfoSectionKey(record: WeaponStockRecord): String = infoSectionKey(record, ADVANCED_INFO_SECTION)

    private fun infoSectionKey(record: WeaponStockRecord, section: String): String = record.itemKey + "::info::" + section

    private fun itemInfo(
        label: String,
        record: WeaponStockRecord,
        section: String,
        expanded: Boolean,
        layout: StockReviewRowLayout,
    ): WimGuiListRow<StockReviewAction> =
        StockReviewListRow.fromSpec(
            StockReviewRowSpecs.nestedHeading(
                WimGuiToggleHeading.label(label, expanded),
                layout.infoIndent,
                layout.rightBlockWidth,
                StockReviewAction.toggleItem(infoSectionKey(record, section)),
                false,
                "Show or hide ${label.lowercase(Locale.US)} rows.",
            ),
        )
}
