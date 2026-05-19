package weaponsprocurement.ui.stockreview.ships

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.PositionAPI
import weaponsprocurement.ui.stockreview.rendering.StockReviewSpriteRenderer
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
        StockReviewSpriteRenderer.renderFittedSprite(
            spriteName,
            Color.WHITE,
            current.x + current.width * 0.5f,
            current.y + current.height * centerYFraction,
            current.width * maxFill,
            current.height * maxFill,
            alphaMult * alphaScale,
        )
    }

}
