package weaponsprocurement.ui.stockreview.actions

class StockReviewActionDispatch private constructor() {
    companion object {
        @JvmStatic
        fun of(vararg groups: StockReviewActionHandlerGroup): StockReviewActionDispatcher =
            StockReviewActionDispatcher(groups.toList())
    }
}

class StockReviewActionDispatcher(
    private val groups: List<StockReviewActionHandlerGroup>,
) {
    fun handle(action: StockReviewAction?): Boolean {
        if (action == null) {
            return false
        }
        for (group in groups) {
            if (group.handle(action)) {
                return true
            }
        }
        return false
    }
}

class StockReviewActionHandlerGroup(
    @JvmField val group: StockReviewActionGroup,
    private val handler: (StockReviewAction) -> Unit,
) {
    @JvmField val name: String = group.label

    fun handle(action: StockReviewAction): Boolean {
        if (action.getGroup() != group) {
            return false
        }
        handler.invoke(action)
        return true
    }

    companion object {
        @JvmStatic
        fun group(
            group: StockReviewActionGroup,
            handler: (StockReviewAction) -> Unit,
        ): StockReviewActionHandlerGroup = StockReviewActionHandlerGroup(group, handler)
    }
}
