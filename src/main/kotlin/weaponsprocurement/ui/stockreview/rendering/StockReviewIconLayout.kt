package weaponsprocurement.ui.stockreview.rendering

/**
 * Visual-centering constants for stock item icons. Weapon and LPC sprites are narrower than
 * their row icon cells, so mathematical center looks slightly off in the Starsector font grid.
 */
object StockReviewIconLayout {
    private const val VISUAL_CENTER_X = 0.46f

    @JvmStatic
    fun visualCenterX(x: Float, width: Float): Float = x + relativeVisualCenterX(width)

    @JvmStatic
    fun relativeVisualCenterX(width: Float): Float = width * VISUAL_CENTER_X
}
