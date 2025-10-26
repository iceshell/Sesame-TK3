package fansirsqi.xposed.sesame

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName

/**
 * 测试基类
 * 提供通用的测试设置和清理
 */
abstract class BaseTest {
    
    @get:Rule
    val testName = TestName()
    
    @Before
    fun baseSetUp() {
        println("▶️ 开始测试: ${testName.methodName}")
        setUp()
    }
    
    @After
    fun baseTearDown() {
        tearDown()
        println("✅ 完成测试: ${testName.methodName}")
    }
    
    /**
     * 子类可以override进行额外设置
     */
    open fun setUp() {}
    
    /**
     * 子类可以override进行额外清理
     */
    open fun tearDown() {}
}
