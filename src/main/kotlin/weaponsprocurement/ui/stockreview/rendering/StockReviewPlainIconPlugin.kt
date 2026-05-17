package weaponsprocurement.ui.stockreview.rendering

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.util.Misc
import weaponsprocurement.ui.WimGuiTooltip
import java.awt.Color

class StockReviewPlainIconPlugin(
    private val spriteName: String?,
) : BaseCustomUIPanelPlugin() {
    private var position: PositionAPI? = null

    override fun positionChanged(position: PositionAPI?) {
        this.position = position
    }

    override fun render(alphaMult: Float) {
        val currentPosition = position ?: return
        val x = currentPosition.x
        val y = currentPosition.y
        val width = currentPosition.width
        val height = currentPosition.height
        Misc.renderQuadAlpha(x, y, width, height, BACKING, alphaMult)
        renderFittedSprite(
            spriteName,
            x + width * 0.5f,
            y + height * 0.5f,
            maxOf(1f, width - 2f * ICON_INSET),
            maxOf(1f, height - 2f * ICON_INSET),
            alphaMult,
        )
    }

    private fun renderFittedSprite(
        path: String?,
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
        sprite.color = Color.WHITE
        sprite.angle = 0f
        sprite.renderAtCenter(centerX, centerY)
        sprite.setSize(oldWidth, oldHeight)
        sprite.alphaMult = oldAlpha
        sprite.color = oldColor
        sprite.angle = oldAngle
        return true
    }

    companion object {
        private val BACKING = Color(0, 0, 0, 210)
        private const val ICON_INSET = 1f
    }
}
