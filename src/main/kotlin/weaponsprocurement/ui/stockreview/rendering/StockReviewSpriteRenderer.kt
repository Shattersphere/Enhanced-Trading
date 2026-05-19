package weaponsprocurement.ui.stockreview.rendering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.graphics.SpriteAPI
import weaponsprocurement.ui.WimGuiTooltip
import java.awt.Color

/**
 * Safe fitted-sprite renderer for row and tooltip plugins. It restores sprite mutable state
 * after rendering because Starsector sprite instances are cached and reused globally.
 */
object StockReviewSpriteRenderer {
    @JvmStatic
    fun renderFittedSprite(
        path: String?,
        color: Color?,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
        alphaMult: Float,
    ): Boolean {
        if (!WimGuiTooltip.hasText(path)) {
            return false
        }
        val sprite: SpriteAPI = try {
            Global.getSettings().getSprite(path)
        } catch (_: RuntimeException) {
            return false
        } ?: return false
        if (sprite.width <= 0f || sprite.height <= 0f) {
            return false
        }

        val oldWidth = sprite.width
        val oldHeight = sprite.height
        val oldAlpha = sprite.alphaMult
        val oldColor = sprite.color
        val oldAngle = sprite.angle
        val scale = minOf(maxOf(1f, maxWidth) / oldWidth, maxOf(1f, maxHeight) / oldHeight)
        sprite.setSize(oldWidth * scale, oldHeight * scale)
        sprite.alphaMult = oldAlpha * alphaMult
        sprite.color = color ?: Color.WHITE
        sprite.angle = 0f
        sprite.renderAtCenter(centerX, centerY)
        sprite.setSize(oldWidth, oldHeight)
        sprite.alphaMult = oldAlpha
        sprite.color = oldColor
        sprite.angle = oldAngle
        return true
    }
}
