package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.WimGuiButtonSpec
import weaponsprocurement.ui.WimGuiControls
import weaponsprocurement.ui.WimGuiListBounds
import weaponsprocurement.ui.WimGuiPanelPlugin
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.rendering.StockReviewFormat
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import weaponsprocurement.ui.stockreview.state.StockReviewState
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object StockReviewShipGridRenderer {
    private const val CARD_MIN_WIDTH = 300f
    private const val CARD_HEIGHT = 132f
    private const val CARD_GAP = 5f
    private const val CARD_PAD = 6f
    private const val BUTTON_WIDTH = 84f
    private const val QUESTION_WIDTH = 30f

    @JvmStatic
    fun render(
        root: CustomPanelAPI,
        snapshot: StockReviewShipSnapshot,
        state: StockReviewState,
        pendingTrades: StockReviewPendingShipTrades,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val spec = StockReviewStyle.TRADE_LIST
        val records = snapshot.allRecords(state.getSortMode())
        val columns = max(1, floor((spec.panelWidth + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP)).toInt())
        val cardWidth = (spec.panelWidth - (columns - 1) * CARD_GAP - 2f * spec.rowHorizontalPad) / columns
        val totalRows = ceil(records.size / columns.toFloat()).toInt()
        val visibleRows = max(1, floor((spec.panelHeight - 2f * spec.rowHorizontalPad) / (CARD_HEIGHT + CARD_GAP)).toInt())
        val maxOffset = max(0, totalRows - visibleRows)
        val offset = min(state.getListScrollOffset(), maxOffset)
        state.setListScrollOffset(offset)

        val listPanel = root.createCustomPanel(spec.panelWidth, spec.panelHeight, WimGuiPanelPlugin(spec.panelFill, spec.panelBorder))
        root.addComponent(listPanel).inTL(spec.panelLeft, spec.panelTop)
        if (records.isEmpty()) {
            WimGuiControls.addLabel(
                listPanel,
                "No local ships are available to buy or sell with the current market settings.",
                StockReviewStyle.TEXT,
                0f,
                spec.panelHeight * 0.5f - 12f,
                spec.panelWidth,
                StockReviewStyle.ROW_HEIGHT,
                Alignment.MID,
            )
            return WimGuiListBounds(0, spec.panelLeft, spec.panelTop, spec.panelWidth, spec.panelHeight)
        }

        val startIndex = offset * columns
        val endIndex = min(records.size, startIndex + visibleRows * columns)
        for (index in startIndex until endIndex) {
            val visibleIndex = index - startIndex
            val row = visibleIndex / columns
            val column = visibleIndex % columns
            val x = spec.rowHorizontalPad + column * (cardWidth + CARD_GAP)
            val y = spec.rowHorizontalPad + row * (CARD_HEIGHT + CARD_GAP)
            renderCard(listPanel, records[index], pendingTrades.contains(records[index].key), x, y, cardWidth, buttons)
        }
        return WimGuiListBounds(maxOffset, spec.panelLeft, spec.panelTop, spec.panelWidth, spec.panelHeight)
    }

    private fun renderCard(
        parent: CustomPanelAPI,
        record: StockReviewShipRecord,
        queued: Boolean,
        x: Float,
        y: Float,
        width: Float,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        val fill = if (queued) queuedFill(record) else StockReviewStyle.PANEL_BACKGROUND
        val card = parent.createCustomPanel(width, CARD_HEIGHT, WimGuiPanelPlugin(fill, StockReviewStyle.ROW_BORDER))
        parent.addComponent(card).inTL(x, y)
        val sprite = card.createCustomPanel(
            width,
            CARD_HEIGHT,
            StockReviewShipSpritePlugin(record.member.hullSpec?.spriteName, 0.64f, 0.55f, 0.98f),
        )
        card.addComponent(sprite).inTL(0f, 0f)

        val title = "${record.displayName()} (${record.member.variant?.designation ?: record.member.hullSpec?.designation ?: ""})"
        WimGuiControls.addLabel(card, title, StockReviewStyle.TEXT, CARD_PAD, 2f, width - 66f, 18f, Alignment.LMID)
        WimGuiControls.addLabel(
            card,
            StockReviewFormat.credits(record.price.finalCredits.toLong()),
            StockReviewStyle.LOAD_BUTTON,
            CARD_PAD,
            20f,
            width - 2f * CARD_PAD,
            18f,
            Alignment.LMID,
        )
        WimGuiControls.addLabel(
            card,
            hullMarkers(record),
            StockReviewStyle.LOAD_BUTTON,
            width - 64f,
            2f,
            58f,
            18f,
            Alignment.RMID,
        )

        val bottomY = CARD_HEIGHT - StockReviewStyle.ACTION_BUTTON_HEIGHT - CARD_PAD
        WimGuiControls.addBoundButton(
            card,
            width - QUESTION_WIDTH - BUTTON_WIDTH - CARD_GAP - CARD_PAD,
            bottomY,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            WimGuiButtonSpec.toggle(
                QUESTION_WIDTH,
                "?",
                StockReviewStyle.TEXT,
                StockReviewAction.debugNoop(),
                Alignment.MID,
                StockReviewStyle.ACTION_BACKGROUND,
                StockReviewStyle.ROW_BORDER,
                "Show ship details.",
                StockReviewShipTooltip(record.member),
            ),
            buttons,
        )
        WimGuiControls.addBoundButton(
            card,
            width - BUTTON_WIDTH - CARD_PAD,
            bottomY,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            WimGuiButtonSpec.toggle(
                BUTTON_WIDTH,
                actionLabel(record, queued),
                StockReviewStyle.TEXT,
                StockReviewAction.toggleShipPlan(record.key),
                Alignment.MID,
                actionFill(record, queued),
                StockReviewStyle.ROW_BORDER,
                actionTooltip(record, queued),
                null,
            ),
            buttons,
        )
        WimGuiControls.addLabel(
            card,
            record.submarketName ?: if (record.isBuy()) "Local stock" else "Player fleet",
            StockReviewStyle.MUTED,
            CARD_PAD,
            bottomY,
            width - QUESTION_WIDTH - BUTTON_WIDTH - 3f * CARD_GAP - 2f * CARD_PAD,
            StockReviewStyle.ACTION_BUTTON_HEIGHT,
            Alignment.LMID,
        )
    }

    private fun actionLabel(record: StockReviewShipRecord, queued: Boolean): String =
        if (queued) "Queued" else if (record.isBuy()) "Buy" else "Sell"

    private fun actionFill(record: StockReviewShipRecord, queued: Boolean): Color =
        if (queued) StockReviewStyle.CONFIRM_BUTTON else if (record.isBuy()) StockReviewStyle.BUY_BUTTON else StockReviewStyle.SELL_BUTTON

    private fun queuedFill(record: StockReviewShipRecord): Color =
        if (record.isBuy()) Color(95, 88, 34, 225) else Color(65, 47, 82, 225)

    private fun actionTooltip(record: StockReviewShipRecord, queued: Boolean): String =
        if (queued) {
            "Remove this ship from the pending plan."
        } else if (record.isBuy()) {
            "Queue buying this exact local-market ship."
        } else {
            "Queue selling this exact player-fleet ship."
        }

    private fun hullMarkers(record: StockReviewShipRecord): String {
        val dp = record.member.deploymentPointsCost.roundToInt()
        val bars = when {
            dp >= 40 -> "||||"
            dp >= 20 -> "|||"
            dp >= 10 -> "||"
            else -> "|"
        }
        return bars
    }
}
