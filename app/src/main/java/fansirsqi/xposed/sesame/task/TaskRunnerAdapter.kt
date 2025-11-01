package fansirsqi.xposed.sesame.task

import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.Model

/**
 * TaskRunner适配器类
 *
 * 为Java代码提供更友好的CoroutineTaskRunner调用方式
 */
class TaskRunnerAdapter {

    private val coroutineTaskRunner: CoroutineTaskRunner

    /**
     * 构造函数 - 使用所有已注册的模型
     */
    constructor() {
        val modelList = Model.modelArray.filterNotNull()
        coroutineTaskRunner = CoroutineTaskRunner(modelList)
    }

    /**
     * 构造函数 - 使用指定的模型列表
     */
    constructor(models: List<Model>) {
        coroutineTaskRunner = CoroutineTaskRunner(models)
    }

    /**
     * 执行任务 - 简化版本
     */
    fun run() {
        run(true, ModelTask.TaskExecutionMode.SEQUENTIAL)
    }

    /**
     * 执行任务 - 完整参数版本
     */
    @JvmOverloads
    fun run(
        isFirst: Boolean,
        mode: ModelTask.TaskExecutionMode = ModelTask.TaskExecutionMode.SEQUENTIAL
    ) {
        run(isFirst, mode, BaseModel.taskExecutionRounds.value ?: 1)
    }

    /**
     * 执行任务 - 包含轮数参数（主方法）
     */
    fun run(isFirst: Boolean, mode: ModelTask.TaskExecutionMode, rounds: Int) {
        coroutineTaskRunner.run(isFirst, rounds)
    }

    /**
     * 停止任务执行器
     */
    fun stop() {
        coroutineTaskRunner.stop()
    }

    companion object {
        /**
         * 静态方法：快速执行所有任务
         */
        @JvmStatic
        fun runAllTasks() {
            runAllTasks(ModelTask.TaskExecutionMode.SEQUENTIAL)
        }

        /**
         * 静态方法：使用指定模式执行所有任务
         */
        @JvmStatic
        fun runAllTasks(mode: ModelTask.TaskExecutionMode) {
            TaskRunnerAdapter().run(true, mode)
        }
    }
}
