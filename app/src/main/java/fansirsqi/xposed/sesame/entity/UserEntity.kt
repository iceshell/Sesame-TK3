package fansirsqi.xposed.sesame.entity

/**
 * 表示一个用户实体，包含用户的基本信息。
 *
 * @property userId 用户 ID
 * @property account 用户的账号
 * @property friendStatus 用户的好友状态（例如：是否是好友等）
 * @property realName 用户的真实姓名
 * @property nickName 用户的昵称
 * @property remarkName 用户的备注名
 */
data class UserEntity(
    val userId: String,
    val account: String,
    val friendStatus: Int,
    val realName: String? = null,
    val nickName: String? = null,
    val remarkName: String? = null
) {
    /**
     * 用于显示的名字，优先使用备注名，若无则使用昵称
     */
    val showName: String = remarkName?.takeIf { it.isNotEmpty() } ?: nickName ?: ""

    /**
     * 用于显示的遮掩名字，真实姓名首字母被遮掩
     */
    val maskName: String = showName + "|" + (realName?.let { 
        if (it.length > 1) "*" + it.substring(1) else it 
    } ?: "")

    /**
     * 用户的全名，格式为：显示名字 | 真实姓名 (账号)
     */
    val fullName: String = "$showName|$realName($account)"

    /**
     * 用户 DTO 类，用于传输数据的简化版本。
     */
    data class UserDto(
        var userId: String? = null,
        var account: String? = null,
        var friendStatus: Int? = null,
        var realName: String? = null,
        var nickName: String? = null,
        var remarkName: String? = null
    ) {
        /**
         * 将 UserDto 转换为 UserEntity 实体
         * @return 转换后的 UserEntity 实体
         */
        fun toEntity(): UserEntity {
            return UserEntity(
                userId = userId ?: "",
                account = account ?: "",
                friendStatus = friendStatus ?: 0,
                realName = realName,
                nickName = nickName,
                remarkName = remarkName
            )
        }
    }
}
