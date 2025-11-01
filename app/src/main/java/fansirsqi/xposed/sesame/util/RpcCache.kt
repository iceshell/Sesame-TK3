package fansirsqi.xposed.sesame.util

import java.util.concurrent.ConcurrentHashMap

/**
 * RPC 请求缓存
 * 用于缓存短期内的重复请求，减少网络开销
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
        val ttl: Long = DEFAULT_TTL
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > ttl
        }
    }
    
    // 使用线程安全的 ConcurrentHashMap
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    
    // 默认缓存时间：5秒
    private const val DEFAULT_TTL = 5000L
    
    // 最大缓存数量，避免内存溢出
    private const val MAX_CACHE_SIZE = 100
    
    /**
     * 生成缓存键
     */
    private fun generateKey(method: String?, data: String?): String {
        return "${method}_${data.hashCode()}"
    }
    
    /**
     * 获取缓存值
     * @param method RPC方法名
     * @param data 请求数据
     * @return 缓存的响应，如果不存在或已过期则返回null
     */
    fun get(method: String?, data: String?): String? {
        if (method == null) return null
        
        val key = generateKey(method, data)
        val entry = cache[key] ?: return null
        
        return if (!entry.isExpired()) {
            Log.runtime("RpcCache", "缓存命中: $method")
            entry.value
        } else {
            // 清除过期缓存
            cache.remove(key)
            null
        }
    }
    
    /**
     * 存入缓存
     * @param method RPC方法名
     * @param data 请求数据
     * @param value 响应值
     * @param ttl 缓存时间（毫秒），默认5秒
     */
    fun put(method: String?, data: String?, value: String, ttl: Long = DEFAULT_TTL) {
        if (method == null || value.isEmpty()) return
        
        // 检查缓存大小，防止内存溢出
        if (cache.size >= MAX_CACHE_SIZE) {
            cleanExpiredEntries()
            
            // 如果清理后还是太大，清除最旧的一半
            if (cache.size >= MAX_CACHE_SIZE) {
                val toRemove = cache.entries
                    .sortedBy { it.value.timestamp }
                    .take(MAX_CACHE_SIZE / 2)
                    .map { it.key }
                toRemove.forEach { cache.remove(it) }
            }
        }
        
        val key = generateKey(method, data)
        cache[key] = CacheEntry(value, System.currentTimeMillis(), ttl)
        Log.runtime("RpcCache", "缓存存入: $method, TTL: ${ttl}ms")
    }
    
    /**
     * 清除指定方法的缓存
     */
    fun invalidate(method: String?) {
        if (method == null) return
        
        cache.keys.removeIf { it.startsWith(method) }
        Log.runtime("RpcCache", "缓存清除: $method")
    }
    
    /**
     * 清除所有缓存
     */
    fun clear() {
        cache.clear()
        Log.runtime("RpcCache", "缓存全部清除")
    }
    
    /**
     * 清除过期的缓存项
     */
    private fun cleanExpiredEntries() {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            Log.runtime("RpcCache", "清除过期缓存: ${expiredKeys.size}个")
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getStats(): String {
        cleanExpiredEntries()
        return "缓存项数: ${cache.size}, 最大容量: $MAX_CACHE_SIZE"
    }
}
