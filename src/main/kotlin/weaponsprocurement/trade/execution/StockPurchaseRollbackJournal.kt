package weaponsprocurement.trade.execution

import com.fs.starfarer.api.campaign.CargoAPI
import weaponsprocurement.stock.item.StockItemCargo
import weaponsprocurement.stock.item.StockItemType
import java.util.ArrayList
import java.util.IdentityHashMap

internal class MutationJournal(
    private val playerCargo: CargoAPI?,
    private val itemType: StockItemType,
    private val itemId: String,
) {
    private val creditsBefore: Float = playerCargo?.credits?.get() ?: 0f
    private val snapshots = ArrayList<CargoSnapshot>()
    private val snapshotsByCargo = IdentityHashMap<CargoAPI, CargoSnapshot>()

    fun recordCargo(cargo: CargoAPI?, label: String) {
        if (cargo == null || snapshotsByCargo.containsKey(cargo)) return
        val snapshot = CargoSnapshot(cargo, itemType, itemId, label)
        snapshotsByCargo[cargo] = snapshot
        snapshots.add(snapshot)
    }

    fun rollback(itemType: StockItemType, itemId: String): RollbackReport {
        var restored = 0
        var failed = 0
        for (snapshot in snapshots) {
            try {
                snapshot.itemCountAtFailure = StockItemCargo.itemCount(snapshot.cargo, itemType, itemId)
                StockItemCargo.reconcileItemCount(snapshot.cargo, itemType, itemId, snapshot.itemCountBefore)
                snapshot.itemCountAfterRollback = StockItemCargo.itemCount(snapshot.cargo, itemType, itemId)
                restored++
            } catch (_: Throwable) {
                failed++
            }
        }
        val creditsAtFailure = playerCargo?.credits?.get() ?: 0f
        var creditsRestored = false
        try {
            if (playerCargo != null) {
                playerCargo.credits.set(creditsBefore)
                creditsRestored = true
            }
        } catch (_: Throwable) {
        }
        val creditsAfterRollback = playerCargo?.credits?.get() ?: creditsAtFailure
        return RollbackReport(
            restored,
            failed,
            creditsRestored,
            countsRestored(),
            creditsBefore,
            creditsAtFailure,
            creditsAfterRollback,
            ArrayList(snapshots),
        )
    }

    private fun countsRestored(): Boolean {
        if (snapshots.isEmpty()) return true
        for (snapshot in snapshots) {
            if (snapshot.itemCountAfterRollback != snapshot.itemCountBefore) return false
        }
        return true
    }
}

internal class CargoSnapshot(
    val cargo: CargoAPI,
    itemType: StockItemType,
    itemId: String,
    label: String?,
) {
    val label: String = if (label == null) "unknown cargo" else label
    val itemCountBefore: Int = StockItemCargo.itemCount(cargo, itemType, itemId)
    var itemCountAtFailure: Int = -1
    var itemCountAfterRollback: Int = -1
}

internal class RollbackReport(
    val restoredCargos: Int,
    val failedCargos: Int,
    val creditsRestored: Boolean,
    val countsRestored: Boolean,
    val creditsBefore: Float,
    val creditsAtFailure: Float,
    val creditsAfterRollback: Float,
    private val snapshots: List<CargoSnapshot>,
) {
    val status: String
        get() = if (failedCargos == 0 && creditsRestored && countsRestored) "PASS" else "FAIL"

    fun legacySummary(): String =
        "rollback=attempted restoredCargos=" + restoredCargos +
            " failedCargos=" + failedCargos +
            " creditsRestored=" + creditsRestored +
            " countsRestored=" + countsRestored +
            " touched=" + touchedSummary()

    fun touchedSummary(): String {
        if (snapshots.isEmpty()) return "none"
        val result = StringBuilder()
        for (i in snapshots.indices) {
            if (i > 0) result.append(";")
            val snapshot = snapshots[i]
            result.append(sanitizeLabel(snapshot.label))
                .append(":")
                .append(snapshot.itemCountBefore)
                .append(">")
                .append(if (snapshot.itemCountAtFailure < 0) "?" else snapshot.itemCountAtFailure.toString())
                .append(">")
                .append(if (snapshot.itemCountAfterRollback < 0) "?" else snapshot.itemCountAfterRollback.toString())
        }
        return result.toString()
    }

    companion object {
        fun none(): RollbackReport = RollbackReport(0, 0, false, false, 0f, 0f, 0f, emptyList())

        private fun sanitizeLabel(label: String): String {
            val result = StringBuilder()
            for (ch in label) {
                result.append(if (ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == ':' || ch == '.') ch else '_')
            }
            return result.toString()
        }
    }
}
