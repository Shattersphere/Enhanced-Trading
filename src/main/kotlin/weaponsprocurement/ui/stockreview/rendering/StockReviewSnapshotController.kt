package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.stock.item.WeaponStockSnapshotBuilder
import weaponsprocurement.ui.WimGuiCampaignDialogHost
import weaponsprocurement.ui.stockreview.state.StockReviewState
import com.fs.starfarer.api.campaign.econ.MarketAPI

class StockReviewSnapshotController(
    private val initialMarket: MarketAPI?,
    private val config: StockReviewConfig,
    private val state: StockReviewState,
    private val renderer: StockReviewRenderer,
    private val snapshotBuilder: WeaponStockSnapshotBuilder = WeaponStockSnapshotBuilder(),
) {
    private var snapshot: WeaponStockSnapshot? = null

    fun current(): WeaponStockSnapshot? = snapshot

    fun hasSnapshot(): Boolean = snapshot != null

    fun rebuild(): Boolean {
        val sourceModeNormalized = state.normalizeSourceMode()
        renderer.invalidateModelCache()
        val host = WimGuiCampaignDialogHost.current()
        snapshot = snapshotBuilder.build(
            host.getSector(),
            host.getCurrentMarketOr(initialMarket),
            config,
            state.getSortMode(),
            state.isIncludeCurrentMarketStorage(),
            state.isIncludeBlackMarket(),
            state.getSourceMode(),
        )
        return sourceModeNormalized
    }
}
