package fansirsqi.xposed.sesame.task.antForest

import org.json.JSONArray
import org.json.JSONObject
import fansirsqi.xposed.sesame.entity.VitalityStore.ExchangeStatus
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.data.Status

/**
 * @author Byseven
 * @apiNote
 * @see 2025/1/20
 */
object Vitality {
    private val TAG = Vitality::class.java.simpleName
    val skuInfo = HashMap<String, JSONObject>()

    @JvmStatic
    fun ItemListByType(labelType: String): JSONArray? {
        var itemInfoVOList: JSONArray? = null
        try {
            val jo = JSONObject(AntForestRpcCall.itemList(labelType))
            if (ResChecker.checkRes("${TAG}查询森林活力值商品列表失败:", jo)) {
                itemInfoVOList = jo.optJSONArray("itemInfoVOList")
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "ItemListByType err")
            Log.printStackTrace(TAG, th)
        }
        return itemInfoVOList
    }

    @JvmStatic
    fun ItemDetailBySpuId(spuId: String) {
        try {
            val jo = JSONObject(AntForestRpcCall.itemDetail(spuId))
            if (ResChecker.checkRes("${TAG}查询森林活力值商品详情失败:", jo)) {
                val ItemDetail = jo.getJSONObject("spuItemInfoVO")
                handleItemDetail(ItemDetail)
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "ItemDetailBySpuId err")
            Log.printStackTrace(TAG, th)
        }
    }

    @JvmStatic
    fun initVitality(labelType: String) {
        try {
            val itemInfoVOList = ItemListByType(labelType)
            if (itemInfoVOList != null) {
                for (i in 0 until itemInfoVOList.length()) {
                    val itemInfoVO = itemInfoVOList.getJSONObject(i)
                    handleVitalityItem(itemInfoVO)
                }
            } else {
                Log.error(TAG, "活力兑换🍃初始化失败！")
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "initVitality err")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun handleVitalityItem(vitalityItem: JSONObject) {
        try {
            val spuId = vitalityItem.optString("spuId")
            val skuModelList = vitalityItem.getJSONArray("skuModelList")
            for (i in 0 until skuModelList.length()) {
                val skuModel = skuModelList.getJSONObject(i)
                val skuId = skuModel.getString("skuId")
                val skuName = skuModel.getString("skuName")
                val price = skuModel.getJSONObject("price").getInt("amount")
                var oderInfo = "$skuName\n价格${price}🍃活力值"
                
                if (skuName.contains("能量雨") || skuName.contains("敦煌") || skuName.contains("保护罩") || 
                    skuName.contains("海洋") || skuName.contains("物种") || skuName.contains("收能量") || skuName.contains("隐身")) {
                    oderInfo = "$skuName\n价格${price}🍃活力值\n每日限时兑1个"
                } else if (skuName == "限时31天内使用31天长效双击卡") {
                    oderInfo = "$skuName\n价格${price}🍃活力值\n每月限时兑1个，记得关，艹"
                }
                
                if (!skuModel.has("spuId")) {
                    skuModel.put("spuId", spuId)
                }
                skuInfo[skuId] = skuModel
                IdMapManager.getInstance(VitalityRewardsMap::class.java).add(skuId, oderInfo)
            }
            UserMap.currentUid?.let { IdMapManager.getInstance(VitalityRewardsMap::class.java).save(it) }
        } catch (th: Throwable) {
            Log.runtime(TAG, "handleVitalityItem err")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun handleItemDetail(ItemDetail: JSONObject) {
        try {
            val spuId = ItemDetail.getString("spuId")
            val skuModelList = ItemDetail.getJSONArray("skuModelList")
            for (i in 0 until skuModelList.length()) {
                val skuModel = skuModelList.getJSONObject(i)
                val skuId = skuModel.getString("skuId")
                val skuName = skuModel.getString("skuName")
                if (!skuModel.has("spuId")) {
                    skuModel.put("spuId", spuId)
                }
                skuInfo[skuId] = skuModel
                IdMapManager.getInstance(VitalityRewardsMap::class.java).add(skuId, skuName)
            }
            UserMap.currentUid?.let { IdMapManager.getInstance(VitalityRewardsMap::class.java).save(it) }
        } catch (th: Throwable) {
            Log.runtime(TAG, "handleItemDetail err:")
            Log.printStackTrace(TAG, th)
        }
    }

    @JvmStatic
    fun handleVitalityExchange(skuId: String): Boolean {
        if (Status.hasFlagToday("forest::VitalityExchangeLimit::$skuId")) {
            Log.record(TAG, "活力兑换🍃[$skuId]今日已达上限，跳过兑换")
            return false
        }

        if (skuInfo.isEmpty()) {
            initVitality("SC_ASSETS")
        }
        
        val sku = skuInfo[skuId]
        if (sku == null) {
            Log.record(TAG, "活力兑换🍃找不到要兑换的权益！")
            return false
        }
        
        try {
            val skuName = sku.getString("skuName")
            val itemStatusList = sku.getJSONArray("itemStatusList")
            for (i in 0 until itemStatusList.length()) {
                val itemStatus = itemStatusList.getString(i)
                val status = ExchangeStatus.valueOf(itemStatus)
                if (status.name == itemStatus) {
                    Log.record(TAG, "活力兑换🍃[$skuName]停止:${status.nickName}")
                    if (ExchangeStatus.REACH_LIMIT.name == itemStatus) {
                        Status.setFlagToday("forest::VitalityExchangeLimit::$skuId")
                        Log.forest("活力兑换🍃[$skuName]已达上限,停止兑换！")
                    }
                    return false
                }
            }
            
            val spuId = sku.getString("spuId")
            if (VitalityExchange(spuId, skuId, skuName)) {
                if (skuName.contains("限时")) {
                    Status.setFlagToday("forest::VitalityExchangeLimit::$skuId")
                }
                return true
            }
            ItemDetailBySpuId(spuId)
        } catch (th: Throwable) {
            Log.runtime(TAG, "VitalityExchange err")
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    @JvmStatic
    fun VitalityExchange(spuId: String, skuId: String, skuName: String): Boolean {
        try {
            if (VitalityExchange(spuId, skuId)) {
                Status.vitalityExchangeToday(skuId)
                val exchangedCount = Status.getVitalityCount(skuId)
                Log.forest("活力兑换🍃[$skuName]#第${exchangedCount}次")
                return true
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "VitalityExchange err:$spuId,$skuId")
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    private fun VitalityExchange(spuId: String, skuId: String): Boolean {
        try {
            val jo = JSONObject(AntForestRpcCall.exchangeBenefit(spuId, skuId))
            if (!jo.optBoolean("success")) {
                val resultCode = jo.optString("resultCode", "")
                if ("QUOTA_USER_NOT_ENOUGH" == resultCode) {
                    Log.forest("活力兑换🍃[兑换次数已达上限]#${jo.optString("resultDesc", "")}")
                    Status.setFlagToday("forest::VitalityExchangeLimit::$skuId")
                    return false
                }
            }
            return ResChecker.checkRes("${TAG}森林活力值兑换失败:", jo)
        } catch (th: Throwable) {
            Log.runtime(TAG, "VitalityExchange err:$spuId,$skuId")
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    @JvmStatic
    fun findSkuInfoBySkuName(spuName: String): JSONObject? {
        try {
            if (skuInfo.isEmpty()) {
                initVitality("SC_ASSETS")
            }
            for ((_, sku) in skuInfo) {
                if (sku.getString("skuName").contains(spuName)) {
                    return sku
                }
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "findSkuInfoBySkuName err:")
            Log.printStackTrace(TAG, e)
        }
        return null
    }
}
