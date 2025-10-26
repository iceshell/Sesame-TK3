package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap

class MemberBenefit(i: String, n: String) : MapperEntity() {

    init {
        id = i
        name = n
    }

    companion object {
        /**
         * 获取会员权益列表（作为MapperEntity列表）
         * 用于Java互操作
         */
        @JvmStatic
        fun getListAsMapperEntity(): List<MapperEntity> = getList()

        @JvmStatic
        fun getList(): List<MemberBenefit> {
            return IdMapManager.getInstance(MemberBenefitsMap::class.java).map
                .map { (key, value) -> MemberBenefit(key, value) }
        }
    }
}