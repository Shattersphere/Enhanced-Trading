package weaponsprocurement.autotrade

import com.fs.starfarer.api.Global

/**
 * Notification paths for auto-trade.
 *
 * There are two distinct surfaces:
 *  - The campaign message feed ([postCampaign] -> `campaignUI.addMessage`) shows as a notification
 *    immediately, even overlaid on the interaction dialog at planet-hail, and persists in the log.
 *    This is the right surface for the arrival summary.
 *  - The floating message display ([post] -> `getMessageDisplay()`) renders over the trade screen
 *    where the player is while trades execute, but only becomes visible on the campaign map - so
 *    it's wrong for the hail summary but right for in-trade-screen confirmations.
 */
object AutoTradeNotify {
    @JvmStatic
    fun post(text: String?) {
        if (text.isNullOrBlank()) return
        val ui = Global.getSector()?.campaignUI ?: return
        val display = ui.messageDisplay
        if (display != null) {
            display.addMessage(text)
        } else {
            ui.addMessage(text)
        }
    }

    /** Post to the campaign message feed - shows immediately on arrival and stays in the log.
     *  This surface ignores newlines and wraps at a narrow fixed width, so keep it to one short
     *  line (no multi-line layout is possible here). */
    @JvmStatic
    fun postCampaign(text: String?) {
        if (text.isNullOrBlank()) return
        Global.getSector()?.campaignUI?.addMessage(text)
    }
}
