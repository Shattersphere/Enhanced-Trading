package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.WimGuiText

internal object StockReviewWeaponTooltipTextRenderer {
    fun addDescription(
        tooltip: TooltipMakerAPI,
        itemId: String?,
        contentWidth: Float,
        pad: Float,
        smallPad: Float,
    ) {
        val description: Description = try {
            Global.getSettings().getDescription(itemId, Description.Type.WEAPON)
        } catch (_: RuntimeException) {
            null
        } ?: return
        val firstPara = description.text1FirstPara
        if (hasText(firstPara)) {
            val text = truncateForTooltipLines(firstPara.trim(), DESCRIPTION_MAX_LINES, contentWidth, tooltip)
            val label = tooltip.addPara(tooltipFormat(text), pad)
            if (hasText(description.text2) && description.text2.trim().startsWith("-")) {
                label.italicize()
            }
        }
        if (hasText(description.text2) && description.text2.trim().startsWith("-")) {
            val label = tooltip.addPara(tooltipFormat(description.text2.trim()), smallPad, mutedColor())
            label.italicize()
        }
    }

    fun addDebugDescription(
        tooltip: TooltipMakerAPI,
        description: String?,
        contentWidth: Float,
        pad: Float,
    ) {
        tooltip.addPara(
            tooltipFormat(truncateForTooltipLines(description, DESCRIPTION_MAX_LINES + 2, contentWidth, tooltip)),
            pad,
            textColor(),
        )
    }

    fun addCustomSpecPara(
        tooltip: TooltipMakerAPI,
        text: String?,
        highlight: String?,
        spec: WeaponSpecAPI,
        contentWidth: Float,
        pad: Float,
        smallPad: Float,
    ) {
        if (!hasText(text)) {
            return
        }
        tooltip.addSpacer(smallPad)
        val rawHighlights = splitHighlights(highlight)
        val substitutedText = substituteFormatSpecifiers(text, rawHighlights, spec)
        val displayText = truncateForTooltipLines(substitutedText, CUSTOM_TEXT_MAX_LINES, contentWidth, tooltip)
        val highlights = visibleHighlights(displayText, rawHighlights)
        if (highlights.isNotEmpty()) {
            val label = tooltip.addPara(tooltipFormat(displayText), pad, textColor(), highlightColor(), *highlights)
            label.setHighlight(*highlights)
            label.setHighlightColor(highlightColor())
            tooltip.addSpacer(smallPad)
            return
        }
        tooltip.addPara(tooltipFormat(displayText), pad, textColor())
        tooltip.addSpacer(smallPad)
    }

    private fun truncateForTooltipLines(text: String?, maxLines: Int, width: Float, tooltip: TooltipMakerAPI): String {
        val source = text?.takeIf { hasText(it) } ?: return text ?: ""
        val normalized = source.trim().replace(Regex("\\s+"), " ")
        return WimGuiText.wrapToWidth(normalized, tooltip, width, maxLines).joinToString("\n")
    }

    private fun splitHighlights(highlight: String?): Array<String> {
        val source = highlight?.takeIf { hasText(it) } ?: return emptyArray()
        val result = ArrayList<String>()
        for (raw in source.split("|")) {
            val trimmed = raw.trim()
            if (trimmed.isNotEmpty()) {
                result.add(trimmed)
            }
        }
        return result.toTypedArray()
    }

    private fun visibleHighlights(text: String?, highlights: Array<String>?): Array<String> {
        if (!hasText(text) || highlights == null || highlights.isEmpty()) {
            return emptyArray()
        }
        val result = ArrayList<String>()
        val source = text ?: return emptyArray()
        for (highlight in highlights) {
            if (hasText(highlight) && source.contains(highlight)) {
                result.add(highlight)
            }
        }
        return result.toTypedArray()
    }

    private fun substituteFormatSpecifiers(text: String?, highlights: Array<String>, spec: WeaponSpecAPI): String {
        if (!hasText(text)) {
            return text ?: ""
        }
        val source = text ?: return ""
        val result = StringBuilder(source.length)
        var highlightIndex = 0
        var i = 0
        while (i < source.length) {
            val c = source[i]
            if (c != '%' || i + 1 >= source.length) {
                result.append(c)
                i++
                continue
            }
            val next = source[i + 1]
            if (next == '%') {
                result.append('%')
                i += 2
                continue
            }
            if (next == 's' || next == 'd' || next == 'f') {
                result.append(formatHighlightValue(highlights, highlightIndex, spec))
                highlightIndex++
                i += 2
                continue
            }
            result.append(c)
            i++
        }
        return result.toString()
    }

    private fun formatHighlightValue(highlights: Array<String>?, index: Int, spec: WeaponSpecAPI?): String {
        if (highlights != null && index >= 0 && index < highlights.size && hasText(highlights[index])) {
            return highlights[index].trim()
        }
        if (index == 0 && spec != null && spec.derivedStats != null) {
            val value = if (spec.isBeam) spec.derivedStats.dps else spec.derivedStats.damagePerShot
            if (validNumber(value) && value > 0f) {
                return formatOneDecimalTrim(value)
            }
        }
        return "?"
    }

    private fun tooltipFormat(value: String?): String = value?.replace("%", "%%") ?: ""

    private fun hasText(value: String?): Boolean = value != null && value.trim().isNotEmpty()

    private fun validNumber(value: Float): Boolean = !value.isNaN() && !value.isInfinite()

    private fun formatOneDecimalTrim(value: Float): String {
        if (!validNumber(value)) {
            return "?"
        }
        val rounded = Math.round(value)
        if (Math.abs(value - rounded) < 0.05f) {
            return rounded.toString()
        }
        return String.format(java.util.Locale.US, "%.1f", value)
    }

    private fun textColor() = StockReviewTooltipPanel.TEXT

    private fun mutedColor() = StockReviewTooltipPanel.MUTED

    private fun highlightColor() = Misc.getHighlightColor()

    private const val DESCRIPTION_MAX_LINES = 4
    private const val CUSTOM_TEXT_MAX_LINES = 3
}
