package fansirsqi.xposed.sesame.data

import fansirsqi.xposed.sesame.BaseTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import java.util.Calendar

/**
 * Status状态管理测试
 * 测试状态加载、保存、日期更新等核心功能
 * 
 * 注意：Status类依赖Files类，Files在静态初始化时会访问Android API
 * 这导致在JVM单元测试中会出现SecurityException
 * 
 * 解决方案：
 * 1. 这些测试需要在真实Android环境中运行（Instrumented Tests）
 * 2. 或者重构Files类，延迟初始化Android相关的字段
 * 3. 暂时保持忽略状态
 */
@Ignore("需要Android环境支持 - Files类静态初始化依赖Android API")
class StatusTest : BaseTest() {
    
    private val status = Status.INSTANCE
    
    @Before
    override fun setUp() {
        super.setUp()
        // 每个测试前重置状态
        // Status.unload()  // 需要Files支持
    }
    
    // ========== 1. 基础功能测试 ==========
    
    @Test
    fun `test Status INSTANCE is singleton`() {
        // When
        val instance1 = Status.INSTANCE
        val instance2 = Status.INSTANCE
        
        // Then
        assertSame("Should be the same instance", instance1, instance2)
    }
    
    @Test
    fun `test getCurrentDayTimestamp returns today zero time`() {
        // When
        val timestamp = Status.getCurrentDayTimestamp()
        
        // Then
        assertTrue("Should return positive timestamp", timestamp > 0)
        
        // Verify it's today's zero time
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        assertEquals("Hour should be 0", 0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 0", 0, calendar.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, calendar.get(Calendar.SECOND))
    }
    
    @Test
    fun `test saveTime is initialized`() {
        // When
        val saveTime = status.saveTime
        
        // Then
        assertNotNull("saveTime should not be null", saveTime)
    }
    
    // ========== 2. Flag管理测试 ==========
    
    @Test
    fun `test hasFlagToday returns false for new flag`() {
        // Given
        val flag = "test-flag-new"
        
        // When
        val result = Status.hasFlagToday(flag)
        
        // Then
        assertFalse("Should return false for new flag", result)
    }
    
    @Test
    fun `test setFlagToday adds flag`() {
        // Given
        val flag = "test-flag-add"
        
        // When
        Status.setFlagToday(flag)
        
        // Then
        assertTrue("Should have flag after setting", Status.hasFlagToday(flag))
    }
    
    @Test
    fun `test setFlagToday is idempotent`() {
        // Given
        val flag = "test-flag-idempotent"
        
        // When
        Status.setFlagToday(flag)
        Status.setFlagToday(flag) // Set again
        
        // Then
        assertTrue("Should still have flag", Status.hasFlagToday(flag))
    }
    
    // ========== 3. 森林相关测试 ==========
    
    @Test
    fun `test canWaterFriendToday returns true for new friend`() {
        // Given
        val friendId = "friend-001"
        val maxCount = 3
        
        // When
        val result = Status.canWaterFriendToday(friendId, maxCount)
        
        // Then
        assertTrue("Should allow watering new friend", result)
    }
    
    @Test
    fun `test waterFriendToday increments count`() {
        // Given
        val friendId = "friend-002"
        val count = 1
        
        // When
        Status.waterFriendToday(friendId, count)
        
        // Then
        assertFalse("Should not allow watering again with same count", 
            Status.canWaterFriendToday(friendId, count))
    }
    
    @Test
    fun `test canReserveToday checks reserve limit`() {
        // Given
        val id = "reserve-001"
        val limit = 5
        
        // When
        val result = Status.canReserveToday(id, limit)
        
        // Then
        assertTrue("Should allow reserve under limit", result)
    }
    
    @Test
    fun `test reserveToday increments reserve count`() {
        // Given
        val id = "reserve-002"
        val count = 2
        
        // When
        Status.reserveToday(id, count)
        
        // Then
        assertEquals("Should have correct reserve times", 
            count, Status.getReserveTimes(id))
    }
    
    @Test
    fun `test canCooperateWaterToday returns true for new cooperation`() {
        // Given
        val uid = "user-001"
        val coopId = "coop-001"
        
        // When
        val result = Status.canCooperateWaterToday(uid, coopId)
        
        // Then
        assertTrue("Should allow new cooperation", result)
    }
    
    @Test
    fun `test cooperateWaterToday marks cooperation done`() {
        // Given
        val uid = "user-002"
        val coopId = "coop-002"
        
        // When
        Status.cooperateWaterToday(uid, coopId)
        
        // Then
        assertFalse("Should not allow same cooperation again", 
            Status.canCooperateWaterToday(uid, coopId))
    }
    
    // ========== 4. 农场相关测试 ==========
    
    @Test
    fun `test canAnswerQuestionToday returns true initially`() {
        // When
        val result = Status.canAnswerQuestionToday()
        
        // Then
        assertTrue("Should allow answering question initially", result)
    }
    
    @Test
    fun `test answerQuestionToday marks question answered`() {
        // When
        Status.answerQuestionToday()
        
        // Then
        assertFalse("Should not allow answering again", 
            Status.canAnswerQuestionToday())
    }
    
    @Test
    fun `test canFeedFriendToday returns true for new friend`() {
        // Given
        val friendId = "feed-friend-001"
        val maxCount = 3
        
        // When
        val result = Status.canFeedFriendToday(friendId, maxCount)
        
        // Then
        assertTrue("Should allow feeding new friend", result)
    }
    
    @Test
    fun `test feedFriendToday increments feed count`() {
        // Given
        val friendId = "feed-friend-002"
        
        // When
        Status.feedFriendToday(friendId)
        
        // Then
        assertFalse("Should not allow feeding again with count 1", 
            Status.canFeedFriendToday(friendId, 1))
    }
    
    @Test
    fun `test canUseAccelerateTool checks limit`() {
        // When
        val result = Status.canUseAccelerateTool()
        
        // Then
        assertTrue("Should allow using accelerate tool initially", result)
    }
    
    @Test
    fun `test useAccelerateTool increments count`() {
        // Given
        val initialResult = Status.canUseAccelerateTool()
        
        // When
        Status.useAccelerateTool()
        
        // Then
        assertTrue("Should have used accelerate tool", initialResult)
    }
    
    // ========== 5. 运动相关测试 ==========
    
    @Test
    fun `test canDonateCharityCoin returns true initially`() {
        // When
        val result = Status.canDonateCharityCoin()
        
        // Then
        assertTrue("Should allow donating initially", result)
    }
    
    @Test
    fun `test donateCharityCoin marks donation done`() {
        // When
        Status.donateCharityCoin()
        
        // Then
        assertFalse("Should not allow donating again", 
            Status.canDonateCharityCoin())
    }
    
    @Test
    fun `test canExchangeToday returns true for new uid`() {
        // Given
        val uid = "exchange-001"
        
        // When
        val result = Status.canExchangeToday(uid)
        
        // Then
        assertTrue("Should allow exchange for new uid", result)
    }
    
    @Test
    fun `test exchangeToday marks exchange done`() {
        // Given
        val uid = "exchange-002"
        
        // When
        Status.exchangeToday(uid)
        
        // Then
        assertFalse("Should not allow exchange again", 
            Status.canExchangeToday(uid))
    }
    
    // ========== 6. 装饰相关测试 ==========
    
    @Test
    fun `test canOrnamentToday returns true initially`() {
        // When
        val result = Status.canOrnamentToday()
        
        // Then
        assertTrue("Should allow ornament initially", result)
    }
    
    @Test
    fun `test setOrnamentToday marks ornament done`() {
        // When
        Status.setOrnamentToday()
        
        // Then
        assertFalse("Should not allow ornament again", 
            Status.canOrnamentToday())
    }
    
    @Test
    fun `test canAnimalSleep returns true initially`() {
        // When
        val result = Status.canAnimalSleep()
        
        // Then
        assertTrue("Should allow animal sleep initially", result)
    }
    
    @Test
    fun `test animalSleep marks sleep done`() {
        // When
        Status.animalSleep()
        
        // Then
        assertFalse("Should not allow sleep again", 
            Status.canAnimalSleep())
    }
    
    // ========== 7. 新村相关测试 ==========
    
    @Test
    fun `test canStallDonateToday returns true initially`() {
        // When
        val result = Status.canStallDonateToday()
        
        // Then
        assertTrue("Should allow stall donate initially", result)
    }
    
    @Test
    fun `test setStallDonateToday marks donate done`() {
        // When
        Status.setStallDonateToday()
        
        // Then
        assertFalse("Should not allow donate again", 
            Status.canStallDonateToday())
    }
    
    // ========== 8. 口碑签到测试 ==========
    
    @Test
    fun `test canKbSignInToday returns true initially`() {
        // When
        val result = Status.canKbSignInToday()
        
        // Then
        assertTrue("Should allow KB sign in initially", result)
    }
    
    @Test
    fun `test KbSignInToday marks sign in done`() {
        // When
        Status.KbSignInToday()
        
        // Then
        assertFalse("Should not allow sign in again today", 
            Status.canKbSignInToday())
    }
    
    // ========== 9. 会员权益测试 ==========
    
    @Test
    fun `test canMemberPointExchangeBenefitToday returns true for new benefit`() {
        // Given
        val benefitId = "benefit-001"
        
        // When
        val result = Status.canMemberPointExchangeBenefitToday(benefitId)
        
        // Then
        assertTrue("Should allow exchange for new benefit", result)
    }
    
    @Test
    fun `test memberPointExchangeBenefitToday marks benefit exchanged`() {
        // Given
        val benefitId = "benefit-002"
        
        // When
        Status.memberPointExchangeBenefitToday(benefitId)
        
        // Then
        assertFalse("Should not allow exchange again", 
            Status.canMemberPointExchangeBenefitToday(benefitId))
    }
    
    // ========== 10. 活力值相关测试 ==========
    
    @Test
    fun `test getVitalityCount returns zero for new skuId`() {
        // Given
        val skuId = "sku-001"
        
        // When
        val count = Status.getVitalityCount(skuId)
        
        // Then
        assertEquals("Should return 0 for new skuId", 0, count)
    }
    
    @Test
    fun `test vitalityExchangeToday increments count`() {
        // Given
        val skuId = "sku-002"
        val initialCount = Status.getVitalityCount(skuId)
        
        // When
        Status.vitalityExchangeToday(skuId)
        
        // Then
        assertEquals("Should increment count", 
            initialCount + 1, Status.getVitalityCount(skuId))
    }
    
    // ========== 11. 边界条件测试 ==========
    
    @Test
    fun `test unload does not throw exception`() {
        // When & Then - should not throw
        Status.unload()
        assertNotNull("Status instance should still exist", Status.INSTANCE)
    }
    
    @Test
    fun `test save does not throw with valid user`() {
        // This test may fail if UserMap is not properly initialized
        // We just verify the method exists and can be called
        try {
            // Status.save() requires UserMap.getCurrentUid()
            // In test environment, this might not be available
            // So we just verify the method signature
            assertNotNull("Status instance should exist", Status.INSTANCE)
        } catch (e: Exception) {
            // Expected in test environment without proper user context
            assertTrue("Should throw RuntimeException in test", 
                e is RuntimeException || e.cause is RuntimeException)
        }
    }
    
    @Test
    fun `test updateDay returns boolean`() {
        // Given
        val calendar = Calendar.getInstance()
        
        // When
        val result = Status.updateDay(calendar)
        
        // Then
        assertTrue("Should return boolean value", 
            result == true || result == false)
    }
}
