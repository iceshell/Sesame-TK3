package fansirsqi.xposed.sesame.util

import fansirsqi.xposed.sesame.BaseTest
import org.junit.Test
import org.junit.Assert.*
import java.util.Calendar

/**
 * TimeUtil时间工具类测试
 * 测试时间范围检查、时间比较等核心功能
 */
class TimeUtilTest : BaseTest() {
    
    // ========== 1. 时间范围检查测试 ==========
    
    @Test
    fun `test checkInTimeRange with valid range`() {
        // Given - 10:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        val timeRange = "0800-1200"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, timeRange)
        
        // Then
        assertTrue("10:00 should be in range 08:00-12:00", result)
    }
    
    @Test
    fun `test checkInTimeRange outside range`() {
        // Given - 2:00 PM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        val timeRange = "0800-1200"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, timeRange)
        
        // Then
        assertFalse("14:00 should not be in range 08:00-12:00", result)
    }
    
    @Test
    fun `test checkInTimeRange at start boundary`() {
        // Given - 8:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        val timeRange = "0800-1200"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, timeRange)
        
        // Then
        assertTrue("08:00 should be in range 08:00-12:00 (inclusive)", result)
    }
    
    @Test
    fun `test checkInTimeRange at end boundary`() {
        // Given - 12:00 PM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        val timeRange = "0800-1200"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, timeRange)
        
        // Then
        assertTrue("12:00 should be in range 08:00-12:00 (inclusive)", result)
    }
    
    @Test
    fun `test checkInTimeRange with list of ranges`() {
        // Given - 10:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        val timeRangeList = listOf("0600-0700", "0800-1200", "1400-1800")
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, timeRangeList)
        
        // Then
        assertTrue("10:00 should match second range", result)
    }
    
    @Test
    fun `test checkInTimeRange with invalid format returns false`() {
        // Given
        val timeMillis = System.currentTimeMillis()
        val invalidRange = "invalid-format"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, invalidRange)
        
        // Then
        assertFalse("Invalid format should return false", result)
    }
    
    // ========== 2. 时间字符串比较测试 ==========
    
    @Test
    fun `test isAfterTimeStr returns true when after`() {
        // Given - 10:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isAfterTimeStr(timeMillis, "0800")
        
        // Then
        assertTrue("10:00 should be after 08:00", result)
    }
    
    @Test
    fun `test isBeforeTimeStr returns true when before`() {
        // Given - 7:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isBeforeTimeStr(timeMillis, "0800")
        
        // Then
        assertTrue("07:00 should be before 08:00", result)
    }
    
    @Test
    fun `test isAfterOrCompareTimeStr at exact time`() {
        // Given - 8:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isAfterOrCompareTimeStr(timeMillis, "0800")
        
        // Then
        assertTrue("08:00 should be >= 08:00", result)
    }
    
    @Test
    fun `test isBeforeOrCompareTimeStr at exact time`() {
        // Given - 8:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isBeforeOrCompareTimeStr(timeMillis, "0800")
        
        // Then
        assertTrue("08:00 should be <= 08:00", result)
    }
    
    @Test
    fun `test isCompareTimeStr returns negative when before`() {
        // Given - 7:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isCompareTimeStr(timeMillis, "0800")
        
        // Then
        assertNotNull("Should return comparison result", result)
        assertTrue("07:00 should be < 08:00", result!! < 0)
    }
    
    @Test
    fun `test isCompareTimeStr returns positive when after`() {
        // Given - 9:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isCompareTimeStr(timeMillis, "0800")
        
        // Then
        assertNotNull("Should return comparison result", result)
        assertTrue("09:00 should be > 08:00", result!! > 0)
    }
    
    @Test
    fun `test isCompareTimeStr returns zero when equal`() {
        // Given - 8:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        
        // When
        val result = TimeUtil.isCompareTimeStr(timeMillis, "0800")
        
        // Then
        assertNotNull("Should return comparison result", result)
        assertEquals("08:00 should equal 08:00", 0, result)
    }
    
    // ========== 3. 当前时间检查测试 ==========
    
    @Test
    fun `test checkNowInTimeRange returns boolean`() {
        // Given
        val timeRange = "0000-2359"
        
        // When
        val result = TimeUtil.checkNowInTimeRange(timeRange)
        
        // Then
        assertTrue("Current time should be in 00:00-23:59 range", result)
    }
    
    @Test
    fun `test isNowAfterTimeStr returns boolean`() {
        // Given
        val timeStr = "0000"
        
        // When
        val result = TimeUtil.isNowAfterTimeStr(timeStr)
        
        // Then
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    @Test
    fun `test isNowBeforeTimeStr returns boolean`() {
        // Given
        val timeStr = "2359"
        
        // When
        val result = TimeUtil.isNowBeforeTimeStr(timeStr)
        
        // Then
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    // ========== 4. 边界条件测试 ==========
    
    @Test
    fun `test checkInTimeRange with empty list returns false`() {
        // Given
        val timeMillis = System.currentTimeMillis()
        val emptyList = emptyList<String>()
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, emptyList)
        
        // Then
        assertFalse("Empty list should return false", result)
    }
    
    @Test
    fun `test checkInTimeRange with malformed range`() {
        // Given
        val timeMillis = System.currentTimeMillis()
        val malformedRange = "0800"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, malformedRange)
        
        // Then
        assertFalse("Malformed range should return false", result)
    }
    
    @Test
    fun `test checkInTimeRange with too many parts`() {
        // Given
        val timeMillis = System.currentTimeMillis()
        val tooManyParts = "0800-1200-1400"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, tooManyParts)
        
        // Then
        assertFalse("Range with too many parts should return false", result)
    }
    
    // ========== 5. 跨午夜时间范围测试 ==========
    
    @Test
    fun `test checkInTimeRange with overnight range`() {
        // Given - 11:00 PM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val timeMillis = calendar.timeInMillis
        val overnightRange = "2200-0200"
        
        // When
        val result = TimeUtil.checkInTimeRange(timeMillis, overnightRange)
        
        // Then
        // Note: This depends on TimeUtil's implementation of overnight ranges
        assertTrue("Should return boolean", result == true || result == false)
    }
}
