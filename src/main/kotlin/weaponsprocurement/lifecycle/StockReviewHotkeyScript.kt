package weaponsprocurement.lifecycle

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.shattersphere.shatterlib.gui.dialog.DialogLifecycleTracker
import com.shattersphere.shatterlib.input.KeyPressLatch
import org.apache.log4j.Logger
import org.lwjgl.input.Keyboard
import weaponsprocurement.ui.stockreview.state.StockReviewLaunchState
import weaponsprocurement.ui.stockreview.rendering.StockReviewPanelPlugin
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.WimGuiCampaignDialogHost
import weaponsprocurement.ui.WimGuiDialogOpener
import weaponsprocurement.stock.market.MarketStockService
import weaponsprocurement.stock.item.StockItemStacks
import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.config.WeaponsProcurementConfig

/**
 * Owns the trade-popup hotkey lifecycle. Opening intentionally requires a market-backed
 * dialog/current market; generic non-market storage mutation semantics are not modeled.
 */
class StockReviewHotkeyScript : EveryFrameScript {
    private val hotkey = KeyPressLatch { Keyboard.isKeyDown(it) }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        if (DIALOG_TRACKER.hasPending()) {
            val pending = DIALOG_TRACKER.consumePending()
            openDialog(pending?.context, pending?.state, "pending-reopen")
            return
        }
        val hotkeyCode = WeaponsProcurementConfig.tradeHotkeyKeyCode()
        if (hotkey.consumePress(hotkeyCode)) {
            // The Luna-configured hotkey is a toggle: pressing it again closes the active popup.
            if (DIALOG_TRACKER.isOpen()) {
                DIALOG_TRACKER.requestClose()
                return
            }
            openFromCurrentDialog("hotkey=${hotkeyName(hotkeyCode)}")
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockReviewHotkeyScript::class.java)
        private val DIALOG_TRACKER = DialogLifecycleTracker<MarketAPI?, StockReviewLaunchState?>()
        private var canOpenFailureLogged = false

        @JvmStatic
        fun markDialogClosed() {
            DIALOG_TRACKER.markClosed()
        }

        @JvmStatic
        fun consumeCloseRequest(): Boolean = DIALOG_TRACKER.consumeCloseRequest()

        @JvmStatic
        fun requestReopen(market: MarketAPI?, state: StockReviewState?) {
            requestReopen(market, StockReviewLaunchState(state, null, false))
        }

        @JvmStatic
        fun requestReopen(market: MarketAPI?, launchState: StockReviewLaunchState?) {
            DIALOG_TRACKER.requestReopen(market, launchState)
        }

        @JvmStatic
        fun canOpenFromCurrentDialog(): Boolean {
            WeaponsProcurementConfig.refreshAndPublishSettings()
            val host = WimGuiCampaignDialogHost.current()
            return host.hasDialog() && canOpenAtCurrentMarket(host.getCurrentMarket())
        }

        @JvmStatic
        fun openFromCurrentDialog(source: String?) {
            if (DIALOG_TRACKER.isOpen()) {
                return
            }
            val host = WimGuiCampaignDialogHost.current()
            if (!host.hasDialog()) {
                host.addMessage("Weapon Stock Review requires an active market dialog or market-backed storage dialog.")
                return
            }

            WeaponsProcurementConfig.refreshAndPublishSettings()
            val market = host.getCurrentMarket()
            if (!canOpenAtCurrentMarket(market)) {
                host.addMessage("Weapon Stock Review requires an active market dialog or market-backed storage dialog.")
                return
            }
            try {
                openDialog(market, null, source)
            } catch (t: RuntimeException) {
                DIALOG_TRACKER.markClosed()
                LOG.error("WP_STOCK_REVIEW open failed", t)
                host.addMessage("Weapon Stock Review failed to open. Check starsector.log.")
            }
        }

        @JvmStatic
        fun canOpenAtCurrentMarket(market: MarketAPI?): Boolean {
            if (market == null) {
                return false
            }
            return try {
                val config = StockReviewConfig.load()
                val stock = MarketStockService().collectCurrentMarketItemStock(market, config.isIncludeBlackMarket())
                for (itemKey in stock.itemKeys()) {
                    for (submarketStock in stock.getSubmarketStocks(itemKey)) {
                        if (submarketStock.isPurchasable() && submarketStock.count > 0) {
                            return true
                        }
                    }
                }
                if (playerHasTradeableCargo()) {
                    return true
                }
                hasEnabledRemoteSource()
            } catch (t: RuntimeException) {
                if (!canOpenFailureLogged) {
                    canOpenFailureLogged = true
                    LOG.warn("WP_STOCK_REVIEW availability check failed", t)
                }
                false
            }
        }

        private fun hasEnabledRemoteSource(): Boolean =
            WeaponsProcurementConfig.isSectorMarketEnabled() || WeaponsProcurementConfig.isFixersMarketEnabled()

        private fun hotkeyName(keyCode: Int): String =
            Keyboard.getKeyName(keyCode)?.takeIf { it.isNotBlank() } ?: keyCode.toString()

        private fun playerHasTradeableCargo(): Boolean {
            val sector = Global.getSector()
            if (sector == null || sector.playerFleet == null) {
                return false
            }
            val cargo: CargoAPI? = sector.playerFleet.cargo
            if (cargo?.stacksCopy == null) {
                return false
            }
            for (stack: CargoStackAPI in cargo.stacksCopy) {
                if (StockItemStacks.isVisibleWeaponStack(stack) || StockItemStacks.isVisibleWingStack(stack)) {
                    return true
                }
            }
            return false
        }

        private fun openDialog(market: MarketAPI?, launchState: StockReviewLaunchState?, source: String?) {
            val host = WimGuiCampaignDialogHost.current()
            if (!host.hasDialog()) {
                DIALOG_TRACKER.markClosed()
                return
            }
            try {
                WeaponsProcurementConfig.refreshAndPublishSettings()
                val panelPlugin = StockReviewPanelPlugin(market, launchState)
                WimGuiDialogOpener.show(
                    host.getDialog(),
                    StockReviewStyle.widthFor(panelPlugin.isReviewMode()),
                    StockReviewStyle.heightFor(panelPlugin.isReviewMode()),
                    panelPlugin,
                )
                DIALOG_TRACKER.markOpen()
                LOG.info("WP_STOCK_REVIEW opened source=$source market=${market?.id ?: "null"}")
            } catch (t: RuntimeException) {
                DIALOG_TRACKER.markClosed()
                LOG.error("WP_STOCK_REVIEW open failed", t)
                host.addMessage("Weapon Stock Review failed to open. Check starsector.log.")
            }
        }
    }
}
