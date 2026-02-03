package fansirsqi.xposed.sesame.hook

import fansirsqi.xposed.sesame.BaseTest
import org.junit.Assert.*
import org.junit.Test

class OfflinePolicyTest : BaseTest() {

    @Test
    fun `shouldBlockRpc blocks during cooldown and auto exits after cooldown`() {
        val previousNowProvider = ApplicationHookConstants.nowProvider
        val initialEventsSize = ApplicationHookConstants.getOfflineEventsSnapshot().size

        try {
            var nowMs = 1_000L
            ApplicationHookConstants.nowProvider = { nowMs }

            ApplicationHookConstants.offline = false
            ApplicationHookConstants.offlineUntilMs = 0L
            ApplicationHookConstants.offlineReason = null
            ApplicationHookConstants.offlineReasonDetail = null
            ApplicationHookConstants.lastOfflineEnterAtMs = 0L
            ApplicationHookConstants.lastOfflineExitAtMs = 0L
            ApplicationHookConstants.lastOfflineEnterReason = null
            ApplicationHookConstants.lastOfflineEnterReasonDetail = null
            ApplicationHookConstants.offlineEnterCount.set(0)
            ApplicationHookConstants.offlineExitCount.set(0)

            ApplicationHookConstants.enterOffline(
                cooldownMs = 5_000L,
                reason = "test_reason",
                detail = "detail_1"
            )

            assertTrue(ApplicationHookConstants.offline)
            assertEquals("test_reason", ApplicationHookConstants.offlineReason)
            assertEquals("detail_1", ApplicationHookConstants.offlineReasonDetail)

            nowMs = 1_000L
            assertTrue(ApplicationHookConstants.shouldBlockRpc())
            assertTrue(ApplicationHookConstants.offline)

            nowMs = 6_001L
            assertFalse(ApplicationHookConstants.shouldBlockRpc())
            assertFalse(ApplicationHookConstants.offline)

            val newEvents = ApplicationHookConstants.getOfflineEventsSnapshot().drop(initialEventsSize)
            assertTrue(newEvents.size >= 2)
            assertEquals(ApplicationHookConstants.OfflineEventType.ENTER, newEvents.first().type)
            assertEquals(ApplicationHookConstants.OfflineEventType.AUTO_EXIT, newEvents.last().type)
            assertEquals("test_reason", newEvents.last().reason)
            assertEquals("detail_1", newEvents.last().detail)
        } finally {
            ApplicationHookConstants.nowProvider = previousNowProvider
        }
    }
}
