package weaponsprocurement.ui.stockreview.rows

import com.fs.starfarer.api.ui.Alignment
import weaponsprocurement.stock.fixer.ShipCatalogRarity
import weaponsprocurement.ui.WimGuiRowCell
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.actions.StockReviewActionGroup
import weaponsprocurement.ui.stockreview.controls.StockReviewActionCells
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import java.awt.Color

object StockReviewDebugCellGroup {
    const val SHIP_RARITY_WIDTH = 104f
    const val SHIP_SIZE_WIDTH = 112f
    const val SHIP_FACTION_WIDTH = 132f
    const val SHIP_MARKET_WIDTH = 190f
    const val SHIP_SUBMARKET_WIDTH = 150f
    const val SHIP_PRICE_WIDTH = 112f

    @JvmStatic
    fun colorSampleInfo(label: String, fill: Color, tooltip: String): WimGuiRowCell<StockReviewAction> =
        info(label, StockReviewStyle.DEBUG_SAMPLE_WIDTH, fill, Alignment.MID, tooltip)

    @JvmStatic
    fun colorSampleButton(label: String, fill: Color, tooltip: String): WimGuiRowCell<StockReviewAction> =
        action(label, StockReviewStyle.DEBUG_SAMPLE_WIDTH, fill, StockReviewAction.debugNoop(), tooltip)

    @JvmStatic
    fun colorValueButton(label: String, fill: Color, action: StockReviewAction, tooltip: String): WimGuiRowCell<StockReviewAction> =
        action(label, StockReviewStyle.DEBUG_VALUE_WIDTH, fill, action, tooltip)

    @JvmStatic
    fun colorPreview(color: Color): WimGuiRowCell<StockReviewAction> =
        info("Color(${color.red}, ${color.green}, ${color.blue})", StockReviewStyle.DEBUG_VALUE_WIDTH, color, Alignment.MID, "Current RGB value for the selected color.")

    @JvmStatic
    fun colorDelta(label: String, fill: Color, action: StockReviewAction, tooltip: String): WimGuiRowCell<StockReviewAction> =
        action(label, StockReviewStyle.DEBUG_DELTA_BUTTON_WIDTH, fill, action, tooltip)

    @JvmStatic
    fun shipRarity(label: String?, fill: Color): WimGuiRowCell<StockReviewAction> =
        info(label, SHIP_RARITY_WIDTH, fill, Alignment.MID)

    @JvmStatic
    fun shipSize(label: String?): WimGuiRowCell<StockReviewAction> =
        info(label, SHIP_SIZE_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.MID)

    @JvmStatic
    fun shipFaction(label: String?): WimGuiRowCell<StockReviewAction> =
        info(label, SHIP_FACTION_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID)

    @JvmStatic
    fun shipMarket(label: String?): WimGuiRowCell<StockReviewAction> =
        info(label, SHIP_MARKET_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID)

    @JvmStatic
    fun shipSubmarket(label: String?): WimGuiRowCell<StockReviewAction> =
        info(label, SHIP_SUBMARKET_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.LMID)

    @JvmStatic
    fun shipPrice(baseUnitPrice: Int): WimGuiRowCell<StockReviewAction> =
        info(StockReviewFormat.credits(baseUnitPrice.toLong()), SHIP_PRICE_WIDTH, StockReviewStyle.CELL_BACKGROUND, Alignment.RMID)

    @JvmStatic
    fun shipRarityFill(rarity: ShipCatalogRarity): Color =
        when (rarity) {
            ShipCatalogRarity.COMMON -> StockReviewStyle.CONFIRM_BUTTON
            ShipCatalogRarity.UNCOMMON -> StockReviewStyle.CELL_BACKGROUND
            ShipCatalogRarity.RARE -> StockReviewStyle.LOAD_BUTTON
            ShipCatalogRarity.VERY_RARE -> StockReviewStyle.CANCEL_BUTTON
            ShipCatalogRarity.UNKNOWN_CUSTOM_SUBMARKET -> StockReviewStyle.PRESET_SCOPE_BUTTON
        }

    @JvmStatic
    fun observedShipLabel(unsupportedOnly: Boolean): String = if (unsupportedOnly) "Custom only" else "Observed"

    private fun info(
        label: String?,
        width: Float,
        fill: Color,
        alignment: Alignment,
        tooltip: String? = null,
    ): WimGuiRowCell<StockReviewAction> =
        StockReviewCellGroup.infoCell(label, width, fill, tooltip, alignment)

    private fun action(
        label: String,
        width: Float,
        fill: Color,
        action: StockReviewAction,
        tooltip: String,
    ): WimGuiRowCell<StockReviewAction> =
        StockReviewActionCells.standard(StockReviewActionGroup.DEBUG_MODE, label, width, fill, action, true, tooltip)
}
