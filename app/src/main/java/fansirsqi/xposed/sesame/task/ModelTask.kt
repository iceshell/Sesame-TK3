package fansirsqi.xposed.sesame.task

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelType
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Notify.setStatusTextExec
import fansirsqi.xposed.sesame.util.Notify.updateNextExecText
import fansirsqi.xposed.sesame.util.StringUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lombok.Setter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 基于协程的抽象任务模型类
 *
 * 这是Sesame-TK框架中的核心任务执行类，提供了以下功能：
 * 1. 基于Kotlin协程的任务生命周期管理（启动、停止、暂停）
 * 2. 协程子任务管理（添加、移除、执行）
 * 3. 任务执行统计和监控
 * 4. 统一的顺序、并行、多轮执行模式
 * 5. 协程调度器管理和任务调度
 * 6. 结构化并发和错误处理
 *
 * 主要组件：
 * - taskScope: 任务协程作用域
 * - childTaskMap: 子任务映射表
 * - executionMutex: 执行互斥锁
 * - runCents: 任务运行次数计数器
 *
 * 使用方式：
 * 继承此类并实现抽象方法：getName(), getFields(), check(), run()
 *
 * @author Sesame-TK Team
 */
abstract class ModelTask : Model() {
    /** 任务协程作用域 */
    private var taskScope: CoroutineScope? = null
    
    /** 子任务映射表，存储当前任务的所有子任务 */
    private val childTaskMap: MutableMap<String, ChildModelTask> = ConcurrentHashMap()
    
    /** 执行互斥锁，防止重复执行 */
    private val executionMutex = Mutex()
    
    /** 任务运行次数计数器 */
    var runCents: Int = 0
        private set
    
    /** 任务是否正在运行 */
    @Volatile
    var isRunning = false

    /** 增加任务运行次数 */
    fun addRunCents() {
        this.runCents += 1
    }

    /**
     * 准备任务执行环境
     */
    override fun prepare() {
        if (taskScope == null) {
            taskScope = CoroutineScope(
                Dispatchers.Default + 
                SupervisorJob() + 
                CoroutineName("ModelTask-${getName()}")
            )
        }
    }

    /**
     * 确保协程作用域初始化
     */
    private fun ensureTaskScope() {
        if (taskScope == null) {
            prepare()
        }
    }

    val id: String
        /** 获取任务ID，默认使用toString()方法  */
        get() = toString()

    /** 获取模型类型，固定返回TASK  */
    override fun getType(): ModelType {
        return ModelType.TASK
    }

    /** 获取任务名称，子类必须实现  */
    abstract override fun getName(): String?

    /** 获取任务字段配置，子类必须实现  */
    abstract override fun getFields(): ModelFields?

    /** 检查任务是否可以执行，子类必须实现  */
    abstract fun check(): Boolean?

    /** 
     * 执行任务的具体逻辑（协程版本）
     * Kotlin子类应该覆盖此方法
     */
    protected open suspend fun runSuspend() {
        // 默认调用Java兼容的run方法
        runJava()
    }

    /** 
     * 执行任务的具体逻辑（Java兼容版本）
     * Java子类应该覆盖此方法
     */
    protected open fun runJava() {
        // 子类必须覆盖 runSuspend() 或 runJava() 之一
        throw NotImplementedError("子类必须实现 runSuspend() 或 runJava() 方法")
    }

    /** 
     * 最终调用的run方法
     * 子类不应该直接覆盖此方法
     */
    suspend fun run() {
        runSuspend()
    }

    /** 检查是否存在指定ID的子任务 */
    fun hasChildTask(childId: String): Boolean {
        return childTaskMap.containsKey(childId)
    }

    /**
     * 添加子任务（协程版本，内部使用）
     * @param childTask 要添加的子任务
     */
    private fun addChildTaskSuspend(childTask: ChildModelTask) {
        ensureTaskScope()
        val childId = childTask.id
        
        // 取消已存在的同ID任务
        childTaskMap[childId]?.cancel()
        
        // 设置父任务引用
        childTask.modelTask = this
        
        // 在协程作用域中启动子任务
        childTask.job = taskScope!!.launch {
            try {
                childTask.run()
            } catch (e: Exception) {
                val taskName = getName() ?: "未知任务"
                // 检查是否是协程取消相关的异常
                if (e.javaClass.name.contains("CancellationException") || 
                    e.message?.contains("cancelled") == true ||
                    e.message?.contains("StandaloneCoroutine") == true) {
                    Log.runtime("子任务协程被取消: $taskName-$childId - ${e.message}")
                    // 协程取消是正常现象，不需要打印堆栈
                } else {
                    Log.printStackTrace("子任务执行异常1: $taskName-$childId", e)
                }
            } finally {
                childTaskMap.remove(childId)
            }
        }
        
        // 存储子任务
        childTaskMap[childId] = childTask
    }

    /**
     * 添加子任务
     * 此方法专为Java代码调用设计，自动处理协程上下文
     * @param childTask 要添加的子任务
     * @return 是否添加成功，始终返回true
     */
    fun addChildTask(childTask: ChildModelTask): Boolean {
        // 确保任务作用域已初始化
        ensureTaskScope()
        // 使用协程作用域启动一个新的协程来处理子任务添加
        taskScope!!.launch {
            addChildTaskSuspend(childTask)
        }
        return true
    }

    /**
     * 移除子任务（协程版本）
     */
    suspend fun removeChildTask(childId: String) {
        childTaskMap[childId]?.let { childTask ->
            childTask.cancel()
            childTaskMap.remove(childId)
        }
    }

    /**
     * 启动任务（协程版本）
     * @param force 是否强制重启
     * @param mode 执行模式
     * @param rounds 执行轮数，默认2轮
     */
    fun startTask(
        force: Boolean = false, 
        mode: TaskExecutionMode = TaskExecutionMode.SEQUENTIAL,
        rounds: Int = 2
    ): Job? {
        ensureTaskScope()
        
        return taskScope!!.launch {
            executionMutex.withLock {
                if (isRunning && !force) {
                    Log.runtime(TAG, "任务 ${getName()} 正在运行，跳过启动")
                    return@withLock
                }
                if (isRunning && force) {
                    Log.runtime(TAG, "强制重启任务 ${getName()}")
                    stopTask()
                }
                if (!isEnable || check() != true) {
                    Log.runtime(TAG, "任务 ${getName()} 不满足执行条件")
                    return@withLock
                }
                try {
                    isRunning = true
                    addRunCents()
                    setStatusTextExec(getName())
                    executeMultiRoundTask(mode, rounds)
                } catch (e: CancellationException) {
                    // 协程取消属于正常控制流程（如停止任务/切换用户），不视为错误
                    Log.runtime(TAG, "任务被取消: ${getName()}")
                } catch (e: Exception) {
                    Log.printStackTrace("任务执行异常: ${getName()}", e)
                } finally {
                    isRunning = false
                    updateNextExecText(-1)
                }
            }
        }
    }

    /**
     * 执行多轮任务
     */
    private suspend fun executeMultiRoundTask(mode: TaskExecutionMode, rounds: Int) {
        val startTime = System.currentTimeMillis()
        val stats = TaskExecutionStats()
        
        for (round in 1..rounds) {
            Log.record(TAG, "开始执行第${round}轮任务: ${getName()}")
            
            // 无论什么模式，都使用顺序执行
            executeSequential(round, stats)
            
            // 轮次间延迟
            if (round < rounds) {
                delay(1000) // 1秒间隔
            }
        }
        
        val endTime = System.currentTimeMillis()
        // 完成统计，补充结束时间
        stats.complete()
        Log.record(TAG, "任务 ${getName()} 完成，总耗时: ${endTime - startTime}ms")
        Log.record(TAG, stats.summary)
    }

    /**
     * 顺序执行
     */
    private suspend fun executeSequential(round: Int, stats: TaskExecutionStats) {
        stats.recordTaskStart("${getName()}-Round$round")
        try {
            run()
            stats.recordTaskEnd("${getName()}-Round$round", true)
        } catch (e: CancellationException) {
            // 本轮被取消，记录为跳过而非失败
            stats.recordSkipped("${getName()}-Round$round")
            Log.runtime(TAG, "任务本轮被取消: ${getName()}-Round$round")
        } catch (e: Exception) {
            stats.recordTaskEnd("${getName()}-Round$round", false)
            throw e
        }
    }


    /**
     * 停止任务（协程版本）
     */
    fun stopTask() {
        runBlocking {
            // 取消所有子任务
            childTaskMap.values.forEach { childTask ->
                try {
                    childTask.cancel()
                } catch (e: Exception) {
                    Log.printStackTrace("取消子任务异常", e)
                }
            }
            childTaskMap.clear()
            
            // 取消协程作用域
            taskScope?.cancel()
            taskScope = null
            
            isRunning = false
        }
    }


    /**
     * 任务执行模式（仅支持顺序执行）
     */
    enum class TaskExecutionMode {
        SEQUENTIAL  // 顺序执行（唯一支持的模式）
    }

    /**
     * 任务执行统计类
     */
    class TaskExecutionStats {
        private val startTime: Long = System.currentTimeMillis()
        private var endTime: Long = 0
        private val taskExecutionTimes: MutableMap<String?, Long?> =
            ConcurrentHashMap<String?, Long?>()
        private val successCount = AtomicInteger(0)
        private val failureCount = AtomicInteger(0)
        private val skippedCount = AtomicInteger(0)

        fun recordTaskStart(taskName: String?) {
            taskExecutionTimes.put(taskName, System.currentTimeMillis())
        }

        fun recordTaskEnd(taskName: String?, success: Boolean) {
            val startTime = taskExecutionTimes[taskName]
            if (startTime != null) {
                val executionTime = System.currentTimeMillis() - startTime
                if (success) {
                    successCount.incrementAndGet()
                    Log.debug("任务[" + taskName + "]执行成功，耗时: " + executionTime + "ms")
                } else {
                    failureCount.incrementAndGet()
                    Log.error("任务[" + taskName + "]执行失败，耗时: " + executionTime + "ms")
                }
            }
        }

        fun recordSkipped(taskName: String?) {
            skippedCount.incrementAndGet()
            Log.debug("任务[$taskName]被跳过")
        }

        fun complete() {
            this.endTime = System.currentTimeMillis()
        }

        @get:SuppressLint("DefaultLocale")
        val summary: String
            get() {
                val totalTime = endTime - startTime
                return String.format(
                    "任务执行统计 - 总耗时: %dms, 成功: %d, 失败: %d, 跳过: %d",
                    totalTime, successCount.get(), failureCount.get(), skippedCount.get()
                )
            }
    }

    /**
     * 协程子任务类
     */
    open class ChildModelTask(
        val id: String,
        val group: String = "DEFAULT",
        private val suspendRunnable: (suspend () -> Unit)? = null,
        val execTime: Long = 0L
    ) {
        @Setter
        var modelTask: ModelTask? = null
        
        /** 协程任务Job */
        var job: Job? = null
        
        /** 是否已取消 */
        @Volatile
        var isCancelled: Boolean = false
            private set

        // 兼容构造函数
        constructor(id: String, runnable: Runnable?) : this(
            id = if (StringUtil.isEmpty(id)) "task-${System.currentTimeMillis()}" else id,
            group = "DEFAULT",
            suspendRunnable = runnable?.let { r -> { r.run() } },
            execTime = 0L
        )
        
        constructor(id: String, execTime: Long) : this(
            id = if (StringUtil.isEmpty(id)) "task-${System.currentTimeMillis()}" else id,
            group = "DEFAULT",
            suspendRunnable = null,
            execTime = execTime
        )
        
        // Java完全兼容的构造函数
        constructor(id: String, group: String, runnable: Runnable, execTime: Long) : this(
            id = if (StringUtil.isEmpty(id)) "task-${System.currentTimeMillis()}" else id,
            group = if (StringUtil.isEmpty(group)) "DEFAULT" else group,
            suspendRunnable = { runnable.run() },
            execTime = execTime
        )

        /**
         * 执行子任务
         */
        suspend fun run() {
            if (isCancelled) return
            
            // 如果有延迟执行时间，先等待
            val delay = execTime - System.currentTimeMillis()
            if (delay > 0) {
                delay(delay)
            }
            
            if (isCancelled) return
            
            // 执行任务逻辑
            try {
                suspendRunnable?.invoke() ?: defaultRun()
            } catch (e: CancellationException) {
                // 任务被取消是正常的协程控制流程，记录日志但不需要打印堆栈
                val parentTaskName = modelTask?.getName() ?: "未知任务"
                Log.runtime("子任务被取消: $parentTaskName-$id")
                // 不重新抛出异常，让任务正常结束
                return
            } catch (e: Exception) {
                val parentTaskName = modelTask?.getName() ?: "未知任务"
                // 检查是否是协程取消相关的异常
                if (e.javaClass.name.contains("CancellationException") || 
                    e.message?.contains("cancelled") == true ||
                    e.message?.contains("StandaloneCoroutine") == true) {
                    Log.runtime("子任务协程被取消: $parentTaskName-$id - ${e.message}")
                    // 协程取消是正常现象，不需要打印堆栈
                    return
                } else {
                    Log.printStackTrace("子任务执行异常2: $parentTaskName-$id", e)
                    throw e
                }
            }
        }

        /**
         * 默认执行逻辑
         * 
         * 当子任务没有提供suspendRunnable时调用此方法。
         * 子类可以重写此方法来提供自定义的任务执行逻辑。
         * 
         * 设计模式：模板方法模式
         * - 基类定义算法骨架
         * - 子类可以重写特定步骤
         * 
         * 示例用法:
         * ```
         * class MyTask(id: String) : ChildModelTask(id) {
         *     override suspend fun defaultRun() {
         *         // 自定义任务逻辑
         *         Log.record("执行自定义任务: $id")
         *         delay(1000)
         *         // 执行业务逻辑...
         *     }
         * }
         * ```
         */
        protected open suspend fun defaultRun() {
            // 默认空实现
            Log.debug("子任务[$id]使用默认空实现运行")
        }

        /**
         * 取消子任务
         */
        fun cancel() {
            isCancelled = true
            job?.cancel()
        }
    }

    companion object {
        /** 日志标签 */
        private const val TAG = "ModelTask"
        
        /** 全局任务管理器协程作用域 */
        private val globalTaskScope = CoroutineScope(
            Dispatchers.Default + SupervisorJob() + CoroutineName("GlobalTaskManager")
        )

        /**
         * 停止所有任务（协程版本）
         */
        @JvmStatic
        fun stopAllTask() {
            globalTaskScope.launch {
                for (model in modelArray) {
                    if (model != null) {
                        try {
                            if (ModelType.TASK == model.type) {
                                (model as ModelTask).stopTask()
                            }
                        } catch (e: Exception) {
                            Log.printStackTrace("停止任务异常", e)
                        }
                    }
                }
            }
        }

        /**
         * 批量启动任务（协程版本）
         * @param tasks 要启动的任务列表
         * @param mode 执行模式
         * @param rounds 执行轮数
         */
        @JvmStatic
        fun startAllTasks(
            tasks: List<ModelTask>,
            mode: TaskExecutionMode = TaskExecutionMode.SEQUENTIAL,
            rounds: Int = 2
        ) {
            globalTaskScope.launch {
                // 无论传入什么模式，都使用顺序执行
                tasks.forEach { task ->
                    task.startTask(false, TaskExecutionMode.SEQUENTIAL, rounds)?.join()
                }
            }
        }
    }
}
