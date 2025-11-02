package fansirsqi.xposed.sesame.task

import android.os.Build
import fansirsqi.xposed.sesame.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 任务基类
 * 提供任务启动、停止、子任务管理等基础功能
 */
abstract class BaseTask {
    
    @Volatile
    var thread: Thread? = null
        private set
    
    /** 任务开始时间 */
    @Volatile
    var taskStartTime: Long = 0
        private set
    
    /** 任务结束时间 */
    @Volatile
    var taskEndTime: Long = 0
        private set
    
    val childTaskMap: MutableMap<String, BaseTask> = ConcurrentHashMap()
    
    open fun getId(): String {
        return toString()
    }
    
    abstract fun check(): Boolean
    
    abstract fun run()
    
    @Synchronized
    fun hasChildTask(childId: String): Boolean {
        return childTaskMap.containsKey(childId)
    }
    
    @Synchronized
    fun getChildTask(childId: String): BaseTask? {
        return childTaskMap[childId]
    }
    
    @Synchronized
    fun addChildTask(childTask: BaseTask) {
        val childId = childTask.getId()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId) { _, value ->
                value?.stopTask()
                childTask.startTask()
                childTask
            }
        } else {
            val oldTask = childTaskMap[childId]
            oldTask?.stopTask()
            childTask.startTask()
            childTaskMap[childId] = childTask
        }
    }
    
    @Synchronized
    fun removeChildTask(childId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId) { _, value ->
                if (value != null) {
                    shutdownAndWait(value.thread, -1, TimeUnit.SECONDS)
                }
                null
            }
        } else {
            val oldTask = childTaskMap[childId]
            if (oldTask != null) {
                shutdownAndWait(oldTask.thread, -1, TimeUnit.SECONDS)
            }
            childTaskMap.remove(childId)
        }
    }
    
    @Synchronized
    fun countChildTask(): Int {
        return childTaskMap.size
    }
    
    fun startTask() {
        startTask(false)
    }
    
    @Synchronized
    fun startTask(force: Boolean) {
        val currentThread = thread
        if (currentThread != null && currentThread.isAlive) {
            if (!force) {
                return
            }
            stopTask()
        }
        
        thread = Thread { run() }
        try {
            if (check()) {
                taskStartTime = System.currentTimeMillis()
                taskEndTime = 0
                thread?.start()
                for (childTask in childTaskMap.values) {
                    childTask?.startTask()
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }
    
    @Synchronized
    fun stopTask() {
        val currentThread = thread
        if (currentThread != null && currentThread.isAlive) {
            shutdownAndWait(currentThread, 5, TimeUnit.SECONDS)
        }
        
        for (childTask in childTaskMap.values) {
            shutdownAndWait(childTask.thread, -1, TimeUnit.SECONDS)
        }
        
        if (taskStartTime > 0 && taskEndTime == 0L) {
            taskEndTime = System.currentTimeMillis()
        }
        
        thread = null
        childTaskMap.clear()
    }
    
    companion object {
        private const val TAG = "BaseTask"
        
        @JvmStatic
        fun shutdownAndWait(thread: Thread?, timeout: Long, unit: TimeUnit) {
            if (thread != null) {
                thread.interrupt()
                if (timeout > -1L) {
                    try {
                        thread.join(unit.toMillis(timeout))
                    } catch (e: InterruptedException) {
                        Log.runtime(TAG, "thread shutdownAndWait err:")
                        Log.printStackTrace(TAG, e)
                    }
                }
            }
        }
        
        @JvmStatic
        fun newInstance(): BaseTask {
            return object : BaseTask() {
                override fun run() {}
                
                override fun check(): Boolean = true
            }
        }
        
        @JvmStatic
        fun newInstance(id: String): BaseTask {
            return object : BaseTask() {
                override fun getId(): String = id
                
                override fun run() {}
                
                override fun check(): Boolean = true
            }
        }
        
        @JvmStatic
        fun newInstance(id: String, runnable: Runnable): BaseTask {
            return object : BaseTask() {
                override fun getId(): String = id
                
                override fun run() {
                    runnable.run()
                }
                
                override fun check(): Boolean = true
            }
        }
    }
}
