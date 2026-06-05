package weaponsprocurement.ui.stockreview.tooltips

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.PositionAPI
import com.shattersphere.shatterlib.starsector.ui.StarsectorSpritePainter
import weaponsprocurement.ui.stockreview.rendering.StockReviewIconLayout
import java.awt.Color

internal class StockReviewTooltipIconPanelPlugin(
    private val spriteName: String?,
    private val inset: Float,
) : BaseCustomUIPanelPlugin() {
    private var position: PositionAPI? = null

    override fun positionChanged(position: PositionAPI?) {
        this.position = position
    }

    override fun render(alphaMult: Float) {
        val currentPosition = position ?: return
        val width = currentPosition.width
        val height = currentPosition.height
        val maxWidth = maxOf(1f, width - 2f * inset)
        val maxHeight = maxOf(1f, height - 2f * inset)
        StarsectorSpritePainter.renderFittedSprite(
            spriteName,
            Color.WHITE,
            StockReviewIconLayout.visualCenterX(currentPosition.x, width),
            currentPosition.y + height * 0.5f,
            maxWidth,
            maxHeight,
            alphaMult,
        )
    }
}
