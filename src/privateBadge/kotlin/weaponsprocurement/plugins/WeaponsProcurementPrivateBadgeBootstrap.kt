package weaponsprocurement.plugins

import com.fs.starfarer.api.EveryFrameScript
import weaponsprocurement.internal.WeaponsProcurementCountUpdater

object WeaponsProcurementPrivateBadgeBootstrap {
    @JvmStatic
    fun createCountUpdater(): EveryFrameScript = WeaponsProcurementCountUpdater()
}
