package fansirsqi.xposed.sesame.hook

import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RpcCache
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

import fansirsqi.xposed.sesame.hook.XposedEnv

object ModuleStatusReporter {

    private const val TAG = "ModuleStatusReporter"

    private const val UPDATE_DEBOUNCE_MS = 800L

    private val updateGeneration = AtomicLong(0)
    private val lastUpdateAtMs = AtomicLong(0)

    fun requestUpdate(trigger: String) {
        val generation = updateGeneration.incrementAndGet()
        GlobalThreadPools.execute {
            delay(UPDATE_DEBOUNCE_MS)
            if (updateGeneration.get() != generation) return@execute
            updateNow(trigger)
        }
    }

    fun updateNow(trigger: String) {
        val now = System.currentTimeMillis()
        val last = lastUpdateAtMs.get()
        if (last != 0L && now - last < UPDATE_DEBOUNCE_MS) return
        lastUpdateAtMs.set(now)

        val file = Files.getTargetFileofDir(Files.MAIN_DIR, "ModuleStatus.json")
        val payload = getStatusSnapshot(trigger)
        val json = runCatching { JsonUtil.copyMapper().writeValueAsString(payload) }
            .getOrElse { "{\"timestamp\":$now,\"error\":\"json_serialize_failed\"}" }

        val ok = Files.write2File(json, file)
        if (!ok) {
            Log.runtime(TAG, "write ModuleStatus.json failed")
        }
    }

    fun getStatusSnapshot(trigger: String? = null): Map<String, Any?> {
        val now = System.currentTimeMillis()

        val offline = ApplicationHookConstants.offline
        val untilMs = ApplicationHookConstants.offlineUntilMs
        val remainMs = if (!offline) {
            0L
        } else if (untilMs <= 0L) {
            -1L
        } else {
            (untilMs - now).coerceAtLeast(0L)
        }

        val events = ApplicationHookConstants.getOfflineEventsSnapshot()
            .takeLast(12)
            .map { e ->
                mapOf(
                    "type" to e.type.name,
                    "atMs" to e.atMs,
                    "cooldownMs" to e.cooldownMs,
                    "untilMs" to e.untilMs,
                    "reason" to e.reason,
                    "detail" to e.detail
                )
            }

        return linkedMapOf(
            "framework" to "Hook",
            "timestamp" to now,
            "packageName" to General.PACKAGE_NAME,
            "process" to XposedEnv.processName,
            "trigger" to trigger,
            "offline" to linkedMapOf(
                "enabled" to offline,
                "untilMs" to untilMs,
                "remainMs" to remainMs,
                "reason" to ApplicationHookConstants.offlineReason,
                "detail" to ApplicationHookConstants.offlineReasonDetail,
                "enterCount" to ApplicationHookConstants.offlineEnterCount.get(),
                "exitCount" to ApplicationHookConstants.offlineExitCount.get(),
                "lastEnterAtMs" to ApplicationHookConstants.lastOfflineEnterAtMs,
                "lastExitAtMs" to ApplicationHookConstants.lastOfflineExitAtMs,
                "events" to events
            ),
            "rpc" to RequestManager.getMetricsSnapshot(),
            "rpcCache" to RpcCache.getMetricsSnapshot()
        )
    }
}
