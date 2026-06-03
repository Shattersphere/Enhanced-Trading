package weaponsprocurement.ui.stockreview.rendering

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import weaponsprocurement.ui.WimGuiButtonBinding
import weaponsprocurement.ui.stockreview.actions.StockReviewAction
import weaponsprocurement.ui.stockreview.state.StockReviewState

object StockReviewItemSearchInput {
    @JvmStatic
    fun render(
        root: CustomPanelAPI,
        state: StockReviewState,
        focused: Boolean,
        buttons: MutableList<WimGuiButtonBinding<StockReviewAction>>,
    ) {
        StockReviewSearchInputSupport.renderItemSearch(root, state, focused, buttons)
    }

    @JvmStatic
    fun process(
        events: List<InputEventAPI>,
        root: CustomPanelAPI?,
        state: StockReviewState,
        focused: Boolean,
    ): StockReviewSearchInputSupport.Result =
        StockReviewSearchInputSupport.processItemSearch(events, root, state, focused)
}
