package weaponsprocurement.ui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import weaponsprocurement.ui.stockreview.rendering.StockReviewStyle
import kotlin.math.ceil

/**
 * Simple text tooltip plus shared screen-height cap used by custom tooltips.
 * The cap keeps large debug/stress tooltips from exceeding the visible game UI while
 * rounding the maximum up to a full line so the bottom line is not half-clipped.
 */
class WimGuiTooltip(text: String?) : TooltipMakerAPI.TooltipCreator {
    private val text: String = text ?: ""

    override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false

    override fun getTooltipWidth(tooltipParam: Any?): Float = WIDTH

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
        tooltip.setParaFontDefault()
        tooltip.setParaFontColor(StockReviewStyle.TEXT)
        tooltip.addPara(cappedText(text, tooltip), 0f)
    }

    companion object {
        private const val MAX_SCREEN_HEIGHT_FRACTION = 0.95f
        private const val FALLBACK_SCREEN_HEIGHT = 1080f
        private const val TEXT_LINE_HEIGHT = 22f
        private const val WIDTH = 320f

        @JvmStatic
        fun hasText(tooltip: String?): Boolean = tooltip != null && tooltip.trim().isNotEmpty()

        @JvmStatic
        fun maxTooltipHeight(): Float = maxTooltipHeight(TEXT_LINE_HEIGHT)

        @JvmStatic
        fun maxTooltipHeight(lineHeight: Float): Float {
            val screenHeight = try {
                Global.getSettings().screenHeight
            } catch (_: RuntimeException) {
                FALLBACK_SCREEN_HEIGHT
            }
            val safeLineHeight = maxOf(1f, lineHeight)
            val rawCap = screenHeight * MAX_SCREEN_HEIGHT_FRACTION
            val lineAlignedCap = ceil(rawCap / safeLineHeight).toFloat() * safeLineHeight
            return maxOf(320f, lineAlignedCap)
        }

        @JvmStatic
        fun capHeight(height: Float): Float = capHeight(height, TEXT_LINE_HEIGHT)

        @JvmStatic
        fun capHeight(height: Float, lineHeight: Float): Float =
            minOf(maxTooltipHeight(lineHeight), maxOf(1f, height))

        private fun cappedText(text: String, tooltip: TooltipMakerAPI): String {
            if (!hasText(text)) {
                return ""
            }
            val maxLines = maxOf(1, (maxTooltipHeight(TEXT_LINE_HEIGHT) / TEXT_LINE_HEIGHT).toInt())
            return WimGuiText.wrapToWidth(text, tooltip, WIDTH, maxLines).joinToString("\n")
        }
    }
}
