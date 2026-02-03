package fansirsqi.xposed.sesame.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * RPC 请求缓存 - LRU策略
 * 用于缓存短期内的重复请求，减少网络开销
 * 
 * 特性：
 * - LRU淘汰策略（最近最少使用）
 * - 线程安全
 * - 自动过期清理
 * - 容量限制
 * 
 * @author Cascade AI
 * @date 2024-11-02
 */
object RpcCache {
    
    /**
     * 缓存项数据类
     */
    private data class CacheEntry(
        val value: String,
        val timestamp: Long,
        val ttl: Long = DEFAULT_TTL,
        var lastAccess: Long = System.currentTimeMillis() // LRU访问时间
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > ttl
        }
    }
    
    // 使用LinkedHashMap实现LRU + ConcurrentHashMap保证线程安全
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val accessOrder = LinkedHashMap<String, Long>(16, 0.75f, true)
    private val lock = ReentrantReadWriteLock()
    
    // 默认缓存时间：5秒
    private const val DEFAULT_TTL = 5000L
    
    // 最大缓存数量，避免内存溢出（提升到1000）
    private const val MAX_CACHE_SIZE = 1000
    
    /**
     * 生成缓存键
     */
    private fun generateKey(method: String?, data: String?): String {
        val dataHash = data?.hashCode() ?: 0
        return "${method}_${dataHash}"
    }
    
    /**
     * 获取缓存值（LRU访问）
     * @param method RPC方法名
     * @param data 请求数据
     * @return 缓存的响应，如果不存在或已过期则返回null
     */
    fun get(method: String?, data: String?): String? {
        if (method == null) return null
        
        val key = generateKey(method, data)
        
        return lock.write {
            val entry = cache[key] ?: return@write null
            
            if (entry.isExpired()) {
                cache.remove(key)
                accessOrder.remove(key)
                return@write null
            }

            entry.lastAccess = System.currentTimeMillis()
            accessOrder[key] = entry.lastAccess
            entry.value
        }
    }
    
    /**
     * 存入缓存（LRU淘汰）
     * @param method RPC方法名
     * @param data 请求数据
     * @param value 响应值
     * @param ttl 缓存时间（毫秒），默认5秒
     */
    fun put(method: String?, data: String?, value: String, ttl: Long = DEFAULT_TTL) {
        if (method == null || value.isEmpty()) return
        
        val key = generateKey(method, data)
        
        lock.write {
            // 检查缓存大小，使用LRU淘汰策略
            if (cache.size >= MAX_CACHE_SIZE) {
                cleanExpiredEntries()
                
                // 如果清理后还是太大，使用LRU淘汰最少使用的
                if (cache.size >= MAX_CACHE_SIZE) {
                    val lruKey = accessOrder.entries.firstOrNull()?.key
                    if (lruKey != null) {
                        cache.remove(lruKey)
                        accessOrder.remove(lruKey)
                        Log.runtime("RpcCache", "LRU淘汰: $lruKey")
                    }
                }
            }
            
            val now = System.currentTimeMillis()
            cache[key] = CacheEntry(value, now, ttl, now)
            accessOrder[key] = now
        }
    }
    
    /**
     * 清除指定方法的缓存
     */
    fun invalidate(method: String?) {
        if (method == null) return
        
        lock.write {
            val keysToRemove = cache.keys.filter { it.startsWith(method) }
            keysToRemove.forEach { 
                cache.remove(it)
                accessOrder.remove(it)
            }
        }
    }
    
    /**
     * 清除所有缓存
     */
    fun clear() {
        lock.write {
            cache.clear()
            accessOrder.clear()
        }
    }
    
    /**
     * 清除过期的缓存项（在写锁内调用）
     */
    private fun cleanExpiredEntries() {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { 
            cache.remove(it)
            accessOrder.remove(it)
        }
        
        if (expiredKeys.isNotEmpty()) {
            Log.runtime("RpcCache", "清除过期缓存: ${expiredKeys.size}个")
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getStats(): String {
        lock.write {
            cleanExpiredEntries()
        }
        return lock.read {
            "缓存项数: ${cache.size}/${MAX_CACHE_SIZE}, 命中率优化: LRU"
        }
    }
}
