package fansirsqi.xposed.sesame.task.ancientTree

import fansirsqi.xposed.sesame.hook.RequestManager

/**
 * 古树保护RPC调用
 */
object AncientTreeRpcCall {

    private const val VERSION = "20230522"

    /**
     * 获取古树首页信息
     *
     * @param selectCityCode 选择的城市代码
     * @return RPC响应字符串
     */
    @JvmStatic
    fun homePage(selectCityCode: String): String {
        return RequestManager.requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.homePage",
            "[{\"cityCode\":\"330100\",\"selectCityCode\":\"$selectCityCode\",\"source\":\"antforesthome\"}]"
        )
    }

    /**
     * 查询可兑换的树木列表
     *
     * @param cityCode 城市代码
     * @return RPC响应字符串
     */
    @JvmStatic
    fun queryTreeItemsForExchange(cityCode: String): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            "[{\"cityCode\":\"$cityCode\",\"itemTypes\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 获取地区详情
     *
     * @param districtCode 地区代码
     * @return RPC响应字符串
     */
    @JvmStatic
    fun districtDetail(districtCode: String): String {
        return RequestManager.requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.districtDetail",
            "[{\"districtCode\":\"$districtCode\",\"source\":\"antforesthome\"}]"
        )
    }

    /**
     * 获取项目详情
     *
     * @param ancientTreeProjectId 古树项目ID
     * @param cityCode 城市代码
     * @return RPC响应字符串
     */
    @JvmStatic
    fun projectDetail(ancientTreeProjectId: String, cityCode: String): String {
        return RequestManager.requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.projectDetail",
            "[{\"ancientTreeProjectId\":\"$ancientTreeProjectId\",\"channel\":\"ONLINE\",\"cityCode\":\"$cityCode\",\"source\":\"ancientreethome\"}]"
        )
    }

    /**
     * 保护古树
     *
     * @param activityId 活动ID
     * @param ancientTreeProjectId 古树项目ID
     * @param cityCode 城市代码
     * @return RPC响应字符串
     */
    @JvmStatic
    fun protect(activityId: String, ancientTreeProjectId: String, cityCode: String): String {
        return RequestManager.requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.protect",
            "[{\"ancientTreeActivityId\":\"$activityId\",\"ancientTreeProjectId\":\"$ancientTreeProjectId\",\"cityCode\":\"$cityCode\",\"source\":\"ancientreethome\"}]"
        )
    }
}
