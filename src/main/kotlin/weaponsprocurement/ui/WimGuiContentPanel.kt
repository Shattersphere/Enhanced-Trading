package weaponsprocurement.ui

import com.fs.starfarer.api.ui.CustomPanelAPI

class WimGuiContentPanel {
    private var content: CustomPanelAPI? = null
    private var owner: CustomPanelAPI? = null

    fun begin(root: CustomPanelAPI?, width: Float, height: Float): CustomPanelAPI? {
        if (root == null) return null
        val existing = content
        if (existing != null && owner === root) {
            root.removeComponent(existing)
        }
        content = root.createCustomPanel(width, height, null)
        owner = root
        return content
    }

    fun attach(root: CustomPanelAPI?) {
        if (root != null && content != null && owner === root) {
            root.addComponent(content).inTL(0f, 0f)
        }
    }
}
