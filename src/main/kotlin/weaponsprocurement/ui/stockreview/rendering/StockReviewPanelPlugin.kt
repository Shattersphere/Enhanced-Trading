package weaponsprocurement.ui.stockreview.rendering

import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiCampaignDialogHost
import weaponsprocurement.ui.WimGuiListBounds
import weaponsprocurement.ui.WimGuiModalPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rows.StockReviewScreenMode
import weaponsprocurement.ui.stockreview.state.StockReviewLaunchState
import weaponsprocurement.ui.stockreview.state.StockReviewModeController
import weaponsprocurement.ui.stockreview.state.StockReviewShipFilterField
import weaponsprocurement.ui.stockreview.state.StockReviewState
import weaponsprocurement.ui.stockreview.trade.StockReviewExecutionController
import weaponsprocurement.ui.stockreview.trade.StockReviewLocalMarketIntent
import weaponsprocurement.ui.stockreview.trade.StockReviewPendingTrades
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeActionDispatcher
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeController
import weaponsprocurement.ui.stockreview.trade.StockReviewTradeWarnings
import weaponsprocurement.ui.stockreview.ships.StockReviewPendingShipTrades
import weaponsprocurement.ui.stockreview.ships.StockReviewShipExecutionController
import weaponsprocurement.ui.stockreview.ships.StockReviewShipFilterModal
import weaponsprocurement.ui.stockreview.ships.StockReviewShipGridRenderer
import weaponsprocurement.ui.stockreview.ships.StockReviewShipHullFilterInput
import weaponsprocurement.ui.stockreview.ships.StockReviewShipSnapshot
import weaponsprocurement.ui.stockreview.ships.StockReviewShipSnapshotBuilder
import weaponsprocurement.ui.stockreview.ships.StockReviewShipTradeController
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import org.apache.log4j.Logger
import weaponsprocurement.trade.execution.StockPurchaseService
import weaponsprocurement.config.StockReviewConfig
import weaponsprocurement.stock.item.WeaponStockSnapshot
import weaponsprocurement.lifecycle.StockReviewHotkeyScript

/**
 * Host/orchestration boundary for the stock-review modal. It wires state, snapshots,
 * controllers, and reopen/close behavior; domain logic should stay in focused collaborators.
 */
class StockReviewPanelPlugin(
    private val initialMarket: MarketAPI?,
    launchState: StockReviewLaunchState?,
) : WimGuiModalPanelPlugin<StockReviewAction>(
    StockReviewAction::class.java,
    StockReviewStyle.widthFor(initialScreenMode(launchState)),
    StockReviewStyle.heightFor(initialScreenMode(launchState)),
    StockReviewStyle.BUTTON_POLL_FRAMES_AFTER_MOUSE_EVENT,
    StockReviewStyle.initialListBounds(initialScreenMode(launchState)),
),
    StockReviewUiController.Host,
    StockReviewTradeController.Host,
    StockReviewShipTradeController.Host,
    StockReviewShipExecutionController.Host,
    StockReviewExecutionController.Host {
    private val config: StockReviewConfig = StockReviewConfig.load()
    private val state: StockReviewState = launchState?.getState()?.let { StockReviewState(it) } ?: StockReviewState(config)
    private val renderer = StockReviewRenderer()
    private val snapshots = StockReviewSnapshotController(initialMarket, config, state, renderer)
    private val shipSnapshotBuilder = StockReviewShipSnapshotBuilder()
    private var currentShipSnapshot: StockReviewShipSnapshot = StockReviewShipSnapshot.EMPTY
    private val purchaseService = StockPurchaseService()
    private val pendingTrades = StockReviewPendingTrades()
    private val pendingShipTrades = StockReviewPendingShipTrades()
    private val localMarketIntent = StockReviewLocalMarketIntent(launchState?.getLocalBuyIntent())
    private val modes: StockReviewModeController
    private val ui: StockReviewUiController
    private val trades: StockReviewTradeController
    private val shipTrades: StockReviewShipTradeController
    private val execution: StockReviewExecutionController
    private val shipExecution: StockReviewShipExecutionController
    private val tradeActionDispatcher: StockReviewTradeActionDispatcher
    private var itemSearchFocused = false
    private var shipHullFilterFocused = false
    private var focusedShipFilterField: StockReviewShipFilterField? = null

    init {
        if (launchState != null) {
            pendingTrades.replaceWith(launchState.getPendingTrades())
            pendingShipTrades.replaceWith(launchState.getPendingShipTrades())
            localMarketIntent.seedFromTrades(pendingTrades.asList())
        }
        modes = StockReviewModeController(reviewMode(launchState))
        ui = StockReviewUiController(state, modes, pendingTrades, pendingShipTrades, localMarketIntent, this)
        trades = StockReviewTradeController(state, pendingTrades, localMarketIntent, this)
        shipTrades = StockReviewShipTradeController(pendingShipTrades, this)
        execution = StockReviewExecutionController(state, pendingTrades, purchaseService, this)
        shipExecution = StockReviewShipExecutionController(pendingShipTrades, this)
        tradeActionDispatcher = StockReviewTradeActionDispatcher(state, trades, shipTrades, execution, shipExecution)
    }

    fun isReviewMode(): Boolean = modes.isReviewMode()

    override fun onInit() {
        StockReviewTradeWarnings.initialize(state)
        rebuildSnapshot()
        updateTradeWarning(null)
    }

    override fun onCloseRequested() {
        ui.handleCloseRequested()
    }

    override fun shouldCloseFromExternalInput(): Boolean = StockReviewHotkeyScript.consumeCloseRequest()

    override fun handleInput(events: List<InputEventAPI>, root: CustomPanelAPI?): Boolean {
        if (modes.currentScreenMode() == StockReviewScreenMode.FILTERS && state.isShipTrading()) {
            itemSearchFocused = false
            shipHullFilterFocused = false
            val result = StockReviewShipFilterModal.process(
                events,
                root,
                StockReviewFilterModalRenderer.modalLeft(state),
                StockReviewFilterModalRenderer.modalTop(state),
                state,
                focusedShipFilterField,
            )
            focusedShipFilterField = result.focusedField
            return result.changed
        }
        focusedShipFilterField = null
        if (modes.currentScreenMode() != StockReviewScreenMode.TRADE) {
            val wasFocused = itemSearchFocused || shipHullFilterFocused
            itemSearchFocused = false
            shipHullFilterFocused = false
            return wasFocused
        }
        if (!state.isShipTrading()) {
            val wasFocused = shipHullFilterFocused
            shipHullFilterFocused = false
            val result = StockReviewItemSearchInput.process(events, root, state, itemSearchFocused)
            itemSearchFocused = result.focused
            return wasFocused || result.changed
        }
        itemSearchFocused = false
        val result = StockReviewShipHullFilterInput.process(events, root, state, shipHullFilterFocused)
        shipHullFilterFocused = result.focused
        return result.changed
    }

    override fun reportDialogDismissed(option: Int) {
        StockReviewHotkeyScript.markDialogClosed()
    }

    override fun canRenderContent(): Boolean = snapshots.hasSnapshot()

    override fun renderContent(
        content: CustomPanelAPI,
        buttonBindings: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val currentSnapshot = snapshots.current() ?: return StockReviewStyle.initialListBounds(modes.currentScreenMode())
        val screenMode = modes.currentScreenMode()
        return renderer.render(
            content,
            currentSnapshot,
            state,
            currentShipSnapshot,
            pendingTrades.asList(),
            pendingShipTrades,
            pendingTrades.getRevision(),
            pendingShipTrades.getRevision(),
            modes.getRevision(),
            screenMode,
            modes.getColorDebugTargetIndex(),
            modes.currentColorDebugDraft(),
            modes.isColorDebugPersistent(),
            itemSearchFocused,
            shipHullFilterFocused,
            focusedShipFilterField,
            buttonBindings,
        )
    }

    override fun onScroll(scrollDelta: Int, maxScrollOffset: Int) {
        val effectiveDelta = if (state.isShipTrading()) {
            StockReviewShipGridRenderer.pageScrollDelta(scrollDelta)
        } else {
            scrollDelta
        }
        state.adjustListScrollOffset(effectiveDelta, maxScrollOffset)
    }

    override fun onRebuildFailed(t: Throwable) {
        LOG.error("WP_STOCK_REVIEW rebuild failed", t)
        reportMessage("Weapon Stock Review failed to render. Check starsector.log.")
    }

    override fun handle(action: StockReviewAction) {
        if (tradeActionDispatcher.handle(action) || ui.handle(action)) {
            return
        }
        logUnhandledAction(action)
    }

    override fun snapshot(): WeaponStockSnapshot? = snapshots.current()

    override fun shipSnapshot(): StockReviewShipSnapshot = currentShipSnapshot

    override fun updateTradeWarning(explicitWarning: String?) {
        StockReviewTradeWarnings.update(snapshots.current(), state, pendingTrades.asList(), explicitWarning)
    }

    override fun requestContentRebuild() {
        rebuildContent()
    }

    override fun currentMaxScrollOffset(): Int = maxScrollOffset()

    override fun sector(): SectorAPI? {
        val host = WimGuiCampaignDialogHost.current()
        return host.getSector()
    }

    override fun market(): MarketAPI? {
        val host = WimGuiCampaignDialogHost.current()
        return host.getCurrentMarketOr(initialMarket)
    }

    override fun playerFleet() = sector()?.playerFleet

    override fun includeBlackMarket(): Boolean = state.isIncludeBlackMarketForShipTrading()

    override fun postMessage(message: String?) {
        reportMessage(message)
    }

    override fun clearLocalMarketIntent() {
        localMarketIntent.clear()
    }

    override fun recaptureLocalMarketIntent() {
        localMarketIntent.captureFromTrades(pendingTrades.asList())
    }

    private fun reportMessage(message: String?) {
        WimGuiCampaignDialogHost.current().addMessage(message)
    }

    private fun logUnhandledAction(action: StockReviewAction) {
        if (unhandledActionWarningLogged) {
            return
        }
        unhandledActionWarningLogged = true
        LOG.warn("WP_STOCK_REVIEW unhandled action type=${action.getType()} group=${action.getGroup().name}")
    }

    override fun rebuildSnapshot() {
        snapshots.rebuild()
        currentShipSnapshot = shipSnapshotBuilder.build(market(), playerFleet(), state.isIncludeBlackMarketForShipTrading())
    }

    override fun exitReviewMode() {
        modes.exitReview(state)
    }

    override fun refreshVanillaCargoScreen() {
        WimGuiCampaignDialogHost.current().refreshCargoCore(
            LOG,
            "WP_STOCK_REVIEW refreshed vanilla cargo screen",
            initialMarket,
        )
    }

    override fun requestReopen(review: Boolean) {
        StockReviewHotkeyScript.requestReopen(
            market(),
            StockReviewLaunchState(state, pendingTrades.asList(), pendingShipTrades.asList(), localMarketIntent.asMap(), review),
        )
    }

    override fun requestClose() {
        close()
    }

    override fun reopen(review: Boolean) {
        requestReopen(review)
        close()
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(StockReviewPanelPlugin::class.java)
        private var unhandledActionWarningLogged = false

        private fun reviewMode(launchState: StockReviewLaunchState?): Boolean = launchState != null && launchState.isReviewMode()

        private fun initialScreenMode(launchState: StockReviewLaunchState?): StockReviewScreenMode =
            if (reviewMode(launchState)) StockReviewScreenMode.REVIEW else StockReviewScreenMode.TRADE
    }
}
