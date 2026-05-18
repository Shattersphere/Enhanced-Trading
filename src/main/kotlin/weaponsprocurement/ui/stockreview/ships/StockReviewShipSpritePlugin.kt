package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.ui.PositionAPI
import weaponsprocurement.ui.WimGuiTooltip
import java.awt.Color

class StockReviewShipSpritePlugin(
    private val spriteName: String?,
    private val maxFill: Float = 0.82f,
    private val centerYFraction: Float = 0.54f,
    private val alphaScale: Float = 1f,
) : BaseCustomUIPanelPlugin() {
    private var position: PositionAPI? = null

    override fun positionChanged(position: PositionAPI?) {
        this.position = position
    }

    override fun renderBelow(alphaMult: Float) {
        val current = position ?: return
        renderFittedSprite(
            spriteName,
            current.x + current.width * 0.5f,
            current.y + current.height * centerYFraction,
            current.width * maxFill,
            current.height * maxFill,
            alphaMult * alphaScale,
        )
    }

    private fun renderFittedSprite(
        path: String?,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
        alphaMult: Float,
    ) {
        if (!WimGuiTooltip.hasText(path)) return
        val sprite: SpriteAPI = try {
            Global.getSettings().getSprite(path)
        } catch (_: RuntimeException) {
            return
        } ?: return
        if (sprite.width <= 0f || sprite.height <= 0f) return
        val oldWidth = sprite.width
        val oldHeight = sprite.height
        val oldAlpha = sprite.alphaMult
        val oldColor = sprite.color
        val oldAngle = sprite.angle
        val scale = minOf(maxWidth / oldWidth, maxHeight / oldHeight)
        sprite.setSize(oldWidth * scale, oldHeight * scale)
        sprite.alphaMult = oldAlpha * alphaMult
        sprite.color = Color.WHITE
        sprite.angle = 0f
        sprite.renderAtCenter(centerX, centerY)
        sprite.setSize(oldWidth, oldHeight)
        sprite.alphaMult = oldAlpha
        sprite.color = oldColor
        sprite.angle = oldAngle
    }
}
