package fansirsqi.xposed.sesame.util.maps

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.entity.UserEntity
import fansirsqi.xposed.sesame.util.ErrorHandler
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
    @Volatile
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
        if (userId.isNullOrBlank()) {
            return null
        }
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
        if (userId.isNullOrBlank()) {
            return null
        }
        return userMap[userId]
    }
    
    /**
     * 添加用户到映射
     *
     * @param userEntity 用户实体
     */
    @JvmStatic
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
        
        ErrorHandler.safelyRun(TAG, "加载用户数据失败") {
            val friendIdMapFile = Files.getFriendIdMapFile(userId) ?: run {
                Log.runtime(TAG, "Friend ID map file is null for userId: $userId")
                return@safelyRun
            }
            
            val body = Files.readFromFile(friendIdMapFile)
            if (body.isNotEmpty()) {
                val dtoMap: Map<String, UserEntity.UserDto> = JsonUtil.parseObject(
                    body,
                    object : TypeReference<Map<String, UserEntity.UserDto>>() {}
                )
                for (dto in dtoMap.values) {
                    val uid = dto.userId ?: continue
                    userMap[uid] = dto.toEntity()
                }
            }
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
        val id = userId ?: run {
            Log.error(TAG, "userId为空，无法保存")
            return false
        }
        
        val file = Files.getFriendIdMapFile(id) ?: run {
            Log.error(TAG, "无法获取用户文件: $id")
            return false
        }
        
        return ErrorHandler.safely(TAG, "保存用户数据失败", fallback = false) {
            val json = JsonUtil.formatJson(userMap)
            Files.write2File(json, file)
        } ?: false
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
        if (userId.isNullOrEmpty()) {
            Log.runtime(TAG, "Skip loading self for empty userId")
            return
        }
        
        ErrorHandler.safelyRun(TAG, "加载当前用户数据失败") {
            val selfFile = Files.getSelfIdFile(userId) ?: return@safelyRun
            val body = Files.readFromFile(selfFile)
            
            if (body.isNotEmpty()) {
                val dto: UserEntity.UserDto = JsonUtil.parseObject(
                    body,
                    object : TypeReference<UserEntity.UserDto>() {}
                )
                val uid = dto.userId ?: return@safelyRun
                userMap[uid] = dto.toEntity()
            }
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
        ErrorHandler.safelyRun(TAG, "保存当前用户数据失败") {
            val uid = userEntity.userId ?: run {
                Log.error(TAG, "userEntity.userId为空")
                return@safelyRun
            }
            
            val file = Files.getSelfIdFile(uid) ?: run {
                Log.error(TAG, "无法获取self文件")
                return@safelyRun
            }
            
            val body = JsonUtil.formatJson(userEntity)
            Files.write2File(body, file)
        }
    }
}
