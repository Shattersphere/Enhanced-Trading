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
            return true
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
    @JvmField val name: String,
    private val types: Set<StockReviewAction.Type>,
    private val handler: (StockReviewAction) -> Unit,
) {
    fun handle(action: StockReviewAction): Boolean {
        if (!types.contains(action.getType())) {
            return false
        }
        handler.invoke(action)
        return true
    }

    companion object {
        @JvmStatic
        fun one(
            name: String,
            type: StockReviewAction.Type,
            handler: (StockReviewAction) -> Unit,
        ): StockReviewActionHandlerGroup = StockReviewActionHandlerGroup(name, setOf(type), handler)

        @JvmStatic
        fun many(
            name: String,
            vararg types: StockReviewAction.Type,
            handler: (StockReviewAction) -> Unit,
        ): StockReviewActionHandlerGroup = StockReviewActionHandlerGroup(name, types.toSet(), handler)
    }
}
