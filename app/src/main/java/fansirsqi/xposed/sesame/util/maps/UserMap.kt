package fansirsqi.xposed.sesame.util.maps

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.entity.UserEntity
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * 用于管理和操作用户数据的映射关系
 * 通常在应用程序中用于处理用户信息，如用户的 ID、昵称、账号、好友列表等
 * 通过该类可以高效地加载、存储和操作用户信息，同时提供线程安全的访问机制
 */
object UserMap {
    
    private val TAG = UserMap::class.java.simpleName
    
    // 存储用户信息的线程安全映射
    private val userMap: MutableMap<String, UserEntity> = ConcurrentHashMap()
    
    // 只读的用户信息映射
    private val readOnlyUserMap: Map<String, UserEntity> = Collections.unmodifiableMap(userMap)
    
    /**
     * 当前用户ID
     */
    @JvmStatic
    var currentUid: String? = null
        private set
    
    /**
     * 获取只读的用户信息映射
     *
     * @return 只读的用户映射
     */
    @JvmStatic
    fun getUserMap(): Map<String, UserEntity> {
        return readOnlyUserMap
    }
    
    /**
     * 获取所有用户ID的集合
     *
     * @return 用户ID集合
     */
    @JvmStatic
    fun getUserIdSet(): Set<String> {
        return userMap.keys
    }
    
    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    @JvmStatic
    @Synchronized
    fun setCurrentUserId(userId: String?) {
        currentUid = if (userId.isNullOrEmpty()) null else userId
    }
    
    /**
     * 获取当前用户的掩码名称
     *
     * @return 掩码名称
     */
    @JvmStatic
    fun getCurrentMaskName(): String? {
        return getMaskName(currentUid)
    }
    
    /**
     * 获取指定用户的掩码名称
     *
     * @param userId 用户ID
     * @return 掩码名称
     */
    @JvmStatic
    fun getMaskName(userId: String?): String? {
        val userEntity = userMap[userId]
        return userEntity?.maskName
    }
    
    /**
     * 获取指定用户实体
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    @JvmStatic
    operator fun get(userId: String?): UserEntity? {
        return userMap[userId]
    }
    
    /**
     * 添加用户到映射
     *
     * @param userEntity 用户实体
     */
    @JvmStatic
    @Synchronized
    fun add(userEntity: UserEntity) {
        if (!userEntity.userId.isNullOrEmpty()) {
            userMap[userEntity.userId] = userEntity
        }
    }
    
    /**
     * 从映射中移除指定用户
     *
     * @param userId 用户ID
     */
    @JvmStatic
    @Synchronized
    fun remove(userId: String?) {
        userMap.remove(userId)
    }
    
    /**
     * 加载用户数据
     *
     * @param userId 用户ID
     */
    @JvmStatic
    @Synchronized
    fun load(userId: String?) {
        userMap.clear()
        if (userId.isNullOrEmpty()) {
            Log.runtime(TAG, "Skip loading user map for empty userId")
            return
        }
        
        try {
            val friendIdMapFile = Files.getFriendIdMapFile(userId)
            if (friendIdMapFile == null) {
                Log.runtime(TAG, "Friend ID map file is null for userId: $userId")
                return
            }
            
            val body = Files.readFromFile(friendIdMapFile)
            if (body.isNotEmpty()) {
                val dtoMap: Map<String, UserEntity.UserDto> = JsonUtil.parseObject(
                    body,
                    object : TypeReference<Map<String, UserEntity.UserDto>>() {}
                )
                for (dto in dtoMap.values) {
                    userMap[dto.userId!!] = dto.toEntity()
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 卸载用户数据
     */
    @JvmStatic
    @Synchronized
    fun unload() {
        userMap.clear()
    }
    
    /**
     * 保存用户数据到文件
     *
     * @param userId 用户ID
     * @return 保存结果
     */
    @JvmStatic
    @Synchronized
    fun save(userId: String?): Boolean {
        return Files.write2File(JsonUtil.formatJson(userMap), Files.getFriendIdMapFile(userId!!)!!)
    }
    
    /**
     * 加载当前用户的数据
     *
     * @param userId 用户ID
     */
    @JvmStatic
    @Synchronized
    fun loadSelf(userId: String?) {
        userMap.clear()
        try {
            val body = Files.readFromFile(Files.getSelfIdFile(userId!!)!!)
            if (body.isNotEmpty()) {
                val dto: UserEntity.UserDto = JsonUtil.parseObject(
                    body,
                    object : TypeReference<UserEntity.UserDto>() {}
                )
                userMap[dto.userId!!] = dto.toEntity()
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }
    
    /**
     * 保存当前用户数据到文件
     *
     * @param userEntity 用户实体
     */
    @JvmStatic
    @Synchronized
    fun saveSelf(userEntity: UserEntity) {
        val body = JsonUtil.formatJson(userEntity)
        Files.write2File(body, Files.getSelfIdFile(userEntity.userId!!)!!)
    }
}
