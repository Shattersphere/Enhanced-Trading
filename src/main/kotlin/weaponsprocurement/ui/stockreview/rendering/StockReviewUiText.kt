package weaponsprocurement.ui.stockreview.rendering

class StockReviewUiText private constructor() {
    companion object {
        @JvmStatic
        fun onOff(enabled: Boolean): String = if (enabled) "On" else "Off"
    }
}
