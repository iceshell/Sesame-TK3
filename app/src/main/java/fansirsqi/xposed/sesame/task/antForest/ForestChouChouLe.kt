package fansirsqi.xposed.sesame.task.antForest

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.maps.UserMap

class ForestChouChouLe {
    
    private val taskTryCount = java.util.concurrent.ConcurrentHashMap<String, AtomicInteger>()

    fun chouChouLe() {
        val startTime = System.currentTimeMillis()
        try {
            val source = "task_entry"
            
            val presetBad = LinkedHashSet<String>()
            presetBad.add("FOREST_NORMAL_DRAW_SHARE")
            presetBad.add("FOREST_ACTIVITY_DRAW_SHARE")
            
            Log.record(TAG, "🎰 开始处理森林抽抽乐")
            
            processKnownScenes(source, presetBad)
            
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "chouChouLe 执行异常", e)
        } finally {
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000
            Log.record(TAG, "✨ 森林抽抽乐处理完毕，总耗时: ${duration}秒")
        }
    }

    private fun processKnownScenes(source: String, presetBad: Set<String>) {
        var totalScenes = 0
        var successScenes = 0
        try {
            val knownScenes = arrayOf(
                arrayOf("2025101301", "ANTFOREST_NORMAL_DRAW", "森林抽抽乐普通版"),
                arrayOf("20251024", "ANTFOREST_ACTIVITY_DRAW", "森林抽抽乐活动版")
            )

            for (scene in knownScenes) {
                totalScenes++
                val activityId = scene[0]
                val sceneCode = scene[1]
                val sceneName = scene[2]
                
                Log.record(TAG, "🎰 开始处理：$sceneName (ActivityId: $activityId, SceneCode: $sceneCode)")
                
                try {
                    processChouChouLeScene(activityId, sceneCode, sceneName, source, presetBad)
                    successScenes++
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "$sceneName 处理异常", e)
                }
                
                if (totalScenes < knownScenes.size) {
                    val randomDelay = RandomUtil.nextInt(2000, 4000).toLong()
                    GlobalThreadPools.sleepCompat(randomDelay)
                }
            }

            Log.record(TAG, "📊 场景处理统计: 总计${totalScenes}个, 成功${successScenes}个")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "processKnownScenes 执行异常", e)
        }
    }

    private fun processChouChouLeScene(
        activityId: String, 
        sceneCode: String, 
        sceneName: String, 
        source: String, 
        presetBad: Set<String>
    ) {
        try {
            var doublecheck: Boolean
            val listSceneCode = "${sceneCode}_TASK"

            var jo = JSONObject(AntForestRpcCall.enterDrawActivityopengreen(activityId, sceneCode, source))
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.error(TAG, "$sceneName - enterDrawActivity 调用失败")
                return
            }

            val drawActivity = jo.getJSONObject("drawActivity")
            val startTime = drawActivity.getLong("startTime")
            val endTime = drawActivity.getLong("endTime")
            
            val currentTime = System.currentTimeMillis()
            if (currentTime < startTime || currentTime > endTime) {
                Log.record(TAG, "$sceneName ⏰ 活动不在有效期内，跳过")
                return
            }

            var loopCount = 0
            val MAX_LOOP = 3
            var taskCompleted = 0
            var taskFailed = 0

            do {
                doublecheck = false
                Log.record("$sceneName 第 ${loopCount + 1} 轮任务处理开始")
                
                val listTaskopengreen = JSONObject(AntForestRpcCall.listTaskopengreen(listSceneCode, source))
                if (ResChecker.checkRes(TAG, listTaskopengreen)) {
                    val taskList = listTaskopengreen.getJSONArray("taskInfoList")
                    Log.record(TAG, "$sceneName 📋 发现 ${taskList.length()} 个任务")

                    for (i in 0 until taskList.length()) {
                        val taskInfo = taskList.getJSONObject(i)
                        val taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo")
                        val bizInfo = JSONObject(taskBaseInfo.getString("bizInfo"))
                        val taskName = bizInfo.getString("title")
                        val taskSceneCode = taskBaseInfo.getString("sceneCode")
                        val taskStatus = taskBaseInfo.getString("taskStatus")
                        val taskType = taskBaseInfo.getString("taskType")

                        val taskRights = taskInfo.getJSONObject("taskRights")
                        val rightsTimes = taskRights.getInt("rightsTimes")
                        val rightsTimesLimit = taskRights.getInt("rightsTimesLimit")

                        Log.record("$sceneName 任务: $taskName [$taskType] 状态: $taskStatus 进度: $rightsTimes/$rightsTimesLimit")

                        if (presetBad.contains(taskType)) {
                            Log.record("$sceneName 已屏蔽任务，跳过：$taskName")
                            continue
                        }

                        if (taskType == "NORMAL_DRAW_EXCHANGE_VITALITY" && taskStatus == TaskStatus.TODO.name) {
                            Log.record("$sceneName 处理活力值兑换任务：$taskName")
                            val sginRes = AntForestRpcCall.exchangeTimesFromTaskopengreen(
                                activityId, sceneCode, source, taskSceneCode, taskType
                            )
                            if (ResChecker.checkRes(TAG, sginRes)) {
                                Log.record(TAG, "$sceneName 🧾 $taskName 兑换成功")
                                doublecheck = true
                            } else {
                                Log.error(TAG, "$sceneName 活力值兑换失败: $taskName")
                            }
                            continue
                        }

                        if ((taskType.startsWith("FOREST_NORMAL_DRAW") || taskType.startsWith("FOREST_ACTIVITY_DRAW")) 
                            && taskStatus == TaskStatus.TODO.name) {
                            val taskDelay = RandomUtil.nextInt(10000, 15000).toLong()
                            Log.record("$sceneName 执行任务延时${taskDelay / 1000}S模拟：$taskName")
                            GlobalThreadPools.sleepCompat(taskDelay)

                            val result = if (taskType.contains("XLIGHT")) {
                                AntForestRpcCall.finishTask4Chouchoule(taskType, taskSceneCode)
                            } else {
                                AntForestRpcCall.finishTaskopengreen(taskType, taskSceneCode)
                            }

                            if (ResChecker.checkRes(TAG, result)) {
                                Log.record(TAG, "$sceneName ✅ $taskName 完成成功")
                                taskCompleted++
                                doublecheck = true
                            } else {
                                Log.error(TAG, "$sceneName 任务完成失败: $taskName")
                                taskFailed++
                                val tryCount = taskTryCount.computeIfAbsent(taskType) { AtomicInteger(0) }.incrementAndGet()
                                if (tryCount > 3) {
                                    Log.record(TAG, "$sceneName ⚠️ 任务 $taskName 多次失败(${tryCount}次)，建议检查")
                                }
                            }
                        }

                        if (taskStatus == TaskStatus.FINISHED.name) {
                            val rewardDelay = RandomUtil.nextInt(2000, 4000).toLong()
                            Log.record("$sceneName 领取奖励延时${rewardDelay / 1000}S:$taskName")
                            GlobalThreadPools.sleepCompat(rewardDelay)
                            val sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskSceneCode, taskType)
                            if (ResChecker.checkRes(TAG, sginRes)) {
                                Log.record(TAG, "$sceneName 🎁 $taskName 奖励领取成功")
                                if (rightsTimesLimit - rightsTimes > 0) {
                                    doublecheck = true
                                }
                            } else {
                                Log.error(TAG, "$sceneName 奖励领取失败: $taskName")
                            }
                        }
                    }
                } else {
                    Log.error(TAG, "$sceneName - listTaskopengreen 调用失败")
                    break
                }
                
                if (doublecheck && loopCount < MAX_LOOP - 1) {
                    val loopDelay = RandomUtil.nextInt(2000, 3000).toLong()
                    Log.record("$sceneName 等待${loopDelay / 1000}秒后继续下一轮检查")
                    GlobalThreadPools.sleepCompat(loopDelay)
                }
                
            } while (doublecheck && ++loopCount < MAX_LOOP)

            if (taskCompleted > 0 || taskFailed > 0) {
                Log.record(TAG, "$sceneName 📊 任务统计: 成功${taskCompleted}个, 失败${taskFailed}个")
            }

            Log.record(TAG, "$sceneName 🎲 开始处理抽奖")
            jo = JSONObject(AntForestRpcCall.enterDrawActivityopengreen(activityId, sceneCode, source))
            if (ResChecker.checkRes(TAG, jo)) {
                var drawAsset = jo.getJSONObject("drawAsset")
                var blance = drawAsset.optInt("blance", 0)
                val totalTimes = drawAsset.optInt("totalTimes", 0)

                Log.record(TAG, "$sceneName 🎫 剩余抽奖次数：$blance/$totalTimes")

                var drawCount = 0
                while (blance > 0 && drawCount < 50) {
                    drawCount++
                    Log.record("$sceneName 第 $drawCount 次抽奖")
                    
                    jo = JSONObject(AntForestRpcCall.drawopengreen(activityId, sceneCode, source, UserMap.currentUid ?: ""))
                    if (ResChecker.checkRes(TAG, jo)) {
                        drawAsset = jo.getJSONObject("drawAsset")
                        val newBlance = drawAsset.getInt("blance")
                        val prizeVO = jo.getJSONObject("prizeVO")
                        val prizeName = prizeVO.getString("prizeName")
                        val prizeNum = prizeVO.getInt("prizeNum")
                        Log.record(TAG, "$sceneName 🎁 抽奖获得: $prizeName×$prizeNum | 剩余次数: $newBlance")
                        
                        blance = newBlance
                        
                        if (blance > 0) {
                            val drawDelay = RandomUtil.nextInt(1500, 3000).toLong()
                            GlobalThreadPools.sleepCompat(drawDelay)
                        }
                    } else {
                        Log.error(TAG, "$sceneName - 第 $drawCount 次抽奖失败")
                        break
                    }
                }
                
                if (drawCount > 0) {
                    Log.record(TAG, "$sceneName ✨ 抽奖完成，共抽奖 $drawCount 次")
                }
            } else {
                Log.error(TAG, "$sceneName - 抽奖前enterDrawActivity调用失败")
            }

        } catch (e: Exception) {
            Log.printStackTrace(TAG, "$sceneName 处理异常", e)
        }
    }

    companion object {
        private const val TAG = "AntForest"
    }
}
