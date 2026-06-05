package weaponsprocurement.ui.stockreview.tooltips

internal data class StockReviewTooltipStatRow(
    val label: String = "",
    val value: String = "",
)

internal data class StockReviewWingTooltipLayout(
    val title: String,
    val manufacturer: String,
    val descriptionText: String,
    val technicalRows: List<StockReviewTooltipStatRow>,
    val systemText: String,
    val armamentsText: String,
)
