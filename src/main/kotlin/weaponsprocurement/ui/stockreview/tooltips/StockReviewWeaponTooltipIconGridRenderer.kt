package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.stockreview.rendering.StockReviewWeaponIconPlugin
import java.awt.Color

internal object StockReviewWeaponTooltipIconGridRenderer {
    fun addWeaponGrid(
        tooltip: TooltipMakerAPI,
        contentWidth: Float,
        spriteName: String?,
        motifType: WeaponAPI.WeaponType?,
        rows: List<StockReviewTooltipStatRow>,
        pad: Float,
    ) {
        addIconGrid(tooltip, contentWidth, spriteName, rows, true, motifType, pad)
    }

    fun addDamageTypeGrid(
        tooltip: TooltipMakerAPI,
        contentWidth: Float,
        damageType: DamageType?,
        rows: List<StockReviewTooltipStatRow>,
        pad: Float,
    ) {
        addIconGrid(tooltip, contentWidth, damageIconSpriteName(damageType), rows, false, null, pad)
    }

    fun addSpriteGrid(
        tooltip: TooltipMakerAPI,
        contentWidth: Float,
        spriteName: String?,
        rows: List<StockReviewTooltipStatRow>,
        pad: Float,
    ) {
        addIconGrid(tooltip, contentWidth, spriteName, rows, false, null, pad)
    }

    private fun addIconGrid(
        tooltip: TooltipMakerAPI,
        contentWidth: Float,
        spriteName: String?,
        rows: List<StockReviewTooltipStatRow>,
        weaponTile: Boolean,
        motifType: WeaponAPI.WeaponType?,
        pad: Float,
    ) {
        if (rows.isEmpty()) {
            return
        }
        val visibleRows = cappedRows(rows, MAX_ICON_GRID_ROWS)
        val gridWidth = contentWidth - ICON_LEFT - ICON_SIZE - ICON_GRID_GAP - 8f
        val height = maxOf(ICON_SIZE + ICON_TOP, visibleRows.size * GRID_ROW_HEIGHT)
        val panel = Global.getSettings().createCustom(contentWidth, height, BaseCustomUIPanelPlugin())
        val icon = panel.createCustomPanel(
            ICON_SIZE,
            ICON_SIZE,
            if (weaponTile) {
                StockReviewWeaponIconPlugin(spriteName, motifType)
            } else {
                StockReviewTooltipIconPanelPlugin(spriteName, ICON_INSET)
            },
        )
        panel.addComponent(icon).inTL(ICON_LEFT, minOf(ICON_TOP, maxOf(0f, height - ICON_SIZE)))

        for (i in visibleRows.indices) {
            val row = visibleRows[i]
            addStatRow(panel, ICON_LEFT + ICON_SIZE + ICON_GRID_GAP, i * GRID_ROW_HEIGHT, gridWidth, GRID_ROW_HEIGHT, row)
        }
        tooltip.addCustom(panel, pad)
        tooltip.addSpacer(GRID_BOTTOM_PAD)
    }

    private fun cappedRows(rows: List<StockReviewTooltipStatRow>, maxRows: Int): List<StockReviewTooltipStatRow> {
        if (rows.size <= maxRows) {
            return rows
        }
        val capped = ArrayList(rows.subList(0, maxOf(1, maxRows)))
        capped[capped.size - 1] = StockReviewTooltipStatRow("", "...")
        return capped
    }

    private fun addStatRow(panel: CustomPanelAPI, x: Float, y: Float, width: Float, height: Float, row: StockReviewTooltipStatRow?) {
        if (row == null) {
            return
        }
        if (!hasText(row.label)) {
            addPanelLabel(panel, row.value, highlightColor(), x, y, width, height, Alignment.RMID)
            return
        }
        StockReviewTooltipPanel.addStatRow(
            panel,
            row.label,
            row.value,
            textColor(),
            highlightColor(),
            x,
            y,
            width,
            height,
            GRID_MIN_LABEL_WIDTH,
            GRID_MAX_LABEL_WIDTH,
            GRID_MIN_VALUE_WIDTH,
        )
    }

    private fun addPanelLabel(
        parent: CustomPanelAPI,
        text: String?,
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alignment: Alignment,
    ) {
        StockReviewTooltipPanel.addLabel(parent, text, color, x, y, width, height, alignment)
    }

    private fun damageIconSpriteName(type: DamageType?): String? {
        var key = "icon_other"
        if (DamageType.KINETIC == type) {
            key = "icon_kinetic"
        } else if (DamageType.HIGH_EXPLOSIVE == type) {
            key = "icon_high_explosive"
        } else if (DamageType.FRAGMENTATION == type) {
            key = "icon_fragmentation"
        } else if (DamageType.ENERGY == type) {
            key = "icon_energy"
        }
        return try {
            Global.getSettings().getSpriteName("ui", key)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun hasText(value: String?): Boolean = value != null && value.trim().isNotEmpty()

    private fun textColor(): Color = StockReviewTooltipPanel.TEXT

    private fun highlightColor(): Color = Misc.getHighlightColor()

    private const val GRID_BOTTOM_PAD = 8f
    private const val GRID_ROW_HEIGHT = 24f
    private const val ICON_SIZE = 92f
    private const val ICON_LEFT = 28f
    private const val ICON_TOP = 12f
    private const val ICON_INSET = 2f
    private const val ICON_GRID_GAP = 28f
    private const val GRID_MIN_LABEL_WIDTH = 108f
    private const val GRID_MAX_LABEL_WIDTH = 252f
    private const val GRID_MIN_VALUE_WIDTH = 86f
    private const val MAX_ICON_GRID_ROWS = 10
}
