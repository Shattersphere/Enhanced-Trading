package weaponsprocurement.ui.stockreview.rendering

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.util.Misc
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
        StockReviewSpriteRenderer.renderFittedSprite(
            spriteName,
            Color.WHITE,
            StockReviewIconLayout.visualCenterX(x, width),
            y + height * 0.5f,
            maxOf(1f, width - 2f * ICON_INSET),
            maxOf(1f, height - 2f * ICON_INSET),
            alphaMult,
        )
    }

    companion object {
        private val BACKING = Color(0, 0, 0, 210)
        private const val ICON_INSET = 1f
    }
}
