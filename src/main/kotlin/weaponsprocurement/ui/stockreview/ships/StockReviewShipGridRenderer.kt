package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.combat.ShipAPI
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
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Compact vanilla-inspired ship grid. Page scrolling and card sizing are tied to the
 * user-confirmed 4-column by 5-row baseline.
 */
object StockReviewShipGridRenderer {
    private const val TARGET_COLUMNS = 4
    private const val TARGET_ROWS = 5
    private const val MIN_CARD_HEIGHT = 116f
    private const val CARD_GAP = 5f
    private const val CARD_PAD = 6f
    private const val BUTTON_WIDTH = 84f
    private const val QUESTION_WIDTH = 30f
    private const val SIZE_LABEL_WIDTH = 92f

    @JvmStatic
    fun render(
        root: CustomPanelAPI,
        snapshot: StockReviewShipSnapshot,
        state: StockReviewState,
        pendingTrades: StockReviewPendingShipTrades,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ): WimGuiListBounds {
        val spec = StockReviewStyle.TRADE_LIST
        val panelHeight = shipPanelHeight(spec.panelTop)
        val records = filterShipRecords(snapshot.allRecords(state.getSortMode()), state)
        val columns = TARGET_COLUMNS
        val cardWidth = (spec.panelWidth - (columns - 1) * CARD_GAP - 2f * spec.rowHorizontalPad) / columns
        val totalRows = ceil(records.size / columns.toFloat()).toInt()
        val visibleRows = TARGET_ROWS
        val cardHeight = max(
            MIN_CARD_HEIGHT,
            (panelHeight - 2f * spec.rowHorizontalPad - (visibleRows - 1) * CARD_GAP) / visibleRows,
        )
        val maxOffset = max(0, totalRows - visibleRows)
        val offset = min(state.getListScrollOffset(), maxOffset)
        state.setListScrollOffset(offset)
        val totalPages = max(1, ceil(totalRows / visibleRows.toFloat()).toInt())
        val currentPage = currentPage(offset, maxOffset, visibleRows, totalPages)

        val listPanel = root.createCustomPanel(spec.panelWidth, panelHeight, WimGuiPanelPlugin(spec.panelFill, spec.panelBorder))
        root.addComponent(listPanel).inTL(spec.panelLeft, spec.panelTop)
        if (records.isEmpty()) {
            val message = if (state.getShipHullFilter().isBlank()) {
                "No local ships are available to buy or sell with the current market settings."
            } else {
                "No ships match the current hull-class filter."
            }
            WimGuiControls.addLabel(
                listPanel,
                message,
                StockReviewStyle.TEXT,
                0f,
                panelHeight * 0.5f - 12f,
                spec.panelWidth,
                StockReviewStyle.ROW_HEIGHT,
                Alignment.MID,
            )
            return WimGuiListBounds(0, spec.panelLeft, spec.panelTop, spec.panelWidth, panelHeight)
        }

        val startIndex = offset * columns
        val endIndex = min(records.size, startIndex + visibleRows * columns)
        for (index in startIndex until endIndex) {
            val visibleIndex = index - startIndex
            val row = visibleIndex / columns
            val column = visibleIndex % columns
            val x = spec.rowHorizontalPad + column * (cardWidth + CARD_GAP)
            val y = spec.rowHorizontalPad + row * (cardHeight + CARD_GAP)
            renderCard(listPanel, records[index], pendingTrades.contains(records[index].key), column, x, y, cardWidth, cardHeight, buttons)
        }
        renderPageIndicator(root, spec.panelLeft, spec.panelWidth, currentPage, totalPages)
        return WimGuiListBounds(maxOffset, spec.panelLeft, spec.panelTop, spec.panelWidth, panelHeight)
    }

    @JvmStatic
    fun pageScrollDelta(scrollDelta: Int): Int =
        when {
            scrollDelta > 0 -> TARGET_ROWS
            scrollDelta < 0 -> -TARGET_ROWS
            else -> 0
        }

    private fun renderCard(
        parent: CustomPanelAPI,
        record: StockReviewShipRecord,
        queued: Boolean,
        column: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        val fill = if (queued) queuedFill(record) else StockReviewStyle.PANEL_BACKGROUND
        val card = parent.createCustomPanel(width, height, WimGuiPanelPlugin(fill, StockReviewStyle.ROW_BORDER))
        parent.addComponent(card).inTL(x, y)
        val sprite = card.createCustomPanel(
            width,
            height,
            StockReviewShipSpritePlugin(record.member.hullSpec?.spriteName, 0.56f, 0.52f, 0.98f),
        )
        card.addComponent(sprite).inTL(0f, 0f)

        WimGuiControls.addLabel(card, hullClassLabel(record), StockReviewStyle.TEXT, CARD_PAD, 2f, width - CARD_PAD - SIZE_LABEL_WIDTH - CARD_GAP, 18f, Alignment.LMID)
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
            sizeLabel(record),
            StockReviewStyle.LOAD_BUTTON,
            width - CARD_PAD - SIZE_LABEL_WIDTH,
            2f,
            SIZE_LABEL_WIDTH,
            18f,
            Alignment.RMID,
        )

        val bottomY = height - StockReviewStyle.ACTION_BUTTON_HEIGHT - CARD_PAD
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
                StockReviewShipTooltip(record),
                tooltipLocationForColumn(column),
            ),
            buttons,
        )
        if (record.isDebug()) {
            WimGuiControls.addLabel(
                card,
                "Debug",
                StockReviewStyle.MUTED,
                width - BUTTON_WIDTH - CARD_PAD,
                bottomY,
                BUTTON_WIDTH,
                StockReviewStyle.ACTION_BUTTON_HEIGHT,
                Alignment.MID,
            )
        } else {
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
        }
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

    private fun hullClassLabel(record: StockReviewShipRecord): String {
        record.debugProfile?.hullClassLabel?.let { return it }
        val hullName = record.member.hullSpec?.hullName?.takeIf { it.isNotBlank() }
        return hullName ?: record.displayName()
    }

    private fun sizeLabel(record: StockReviewShipRecord): String {
        record.debugProfile?.sizeLabel?.let { return it }
        return when (record.member.hullSpec?.hullSize) {
            ShipAPI.HullSize.FRIGATE -> "Frigate"
            ShipAPI.HullSize.DESTROYER -> "Destroyer"
            ShipAPI.HullSize.CRUISER -> "Cruiser"
            ShipAPI.HullSize.CAPITAL_SHIP -> "Capital"
            else -> "Unknown"
        }
    }

    private fun tooltipLocationForColumn(column: Int): TooltipMakerAPI.TooltipLocation =
        if (column < TARGET_COLUMNS / 2) {
            TooltipMakerAPI.TooltipLocation.RIGHT
        } else {
            TooltipMakerAPI.TooltipLocation.LEFT
        }

    private fun currentPage(offset: Int, maxOffset: Int, visibleRows: Int, totalPages: Int): Int {
        if (totalPages <= 1 || maxOffset <= 0) return 1
        if (offset >= maxOffset) return totalPages
        return min(totalPages, offset / visibleRows + 1)
    }

    private fun renderPageIndicator(parent: CustomPanelAPI, panelLeft: Float, width: Float, currentPage: Int, totalPages: Int) {
        if (totalPages <= 1) return
        WimGuiControls.addLabel(
            parent,
            "Page $currentPage/$totalPages",
            StockReviewStyle.MUTED,
            panelLeft + width - 150f,
            StockReviewStyle.MODAL.footerButtonY(StockReviewStyle.ACTION_BUTTON_HEIGHT),
            140f,
            StockReviewStyle.ROW_HEIGHT,
            Alignment.RMID,
        )
    }

    private fun shipPanelHeight(panelTop: Float): Float {
        val footerTop = StockReviewStyle.MODAL.footerButtonY(StockReviewStyle.ACTION_BUTTON_HEIGHT)
        return max(
            TARGET_ROWS * MIN_CARD_HEIGHT + (TARGET_ROWS - 1) * CARD_GAP + 2f * StockReviewStyle.SMALL_PAD,
            footerTop - StockReviewStyle.SECTION_GAP - panelTop,
        )
    }

    private fun filterShipRecords(records: List<StockReviewShipRecord>, state: StockReviewState): List<StockReviewShipRecord> =
        filterByHullClass(records, state.getShipHullFilter()).filter { StockReviewShipFilters.matches(it, state) }

    private fun filterByHullClass(records: List<StockReviewShipRecord>, filter: String): List<StockReviewShipRecord> {
        val tokens = filter.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            return records
        }
        return records.filter { record ->
            val searchable = listOfNotNull(
                record.member.hullSpec?.hullName,
                record.member.hullSpec?.nameWithDesignationWithDashClass,
                record.member.hullSpec?.hullId,
                record.member.specId,
            ).joinToString(" ").lowercase(Locale.ROOT)
            tokens.all { searchable.contains(it) }
        }
    }
}
