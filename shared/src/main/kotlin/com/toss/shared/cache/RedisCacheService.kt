package com.toss.shared.cache

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

@Service
class RedisCacheService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    
    fun set(key: String, value: Any, ttl: Duration = Duration.ofHours(1)) {
        redisTemplate.opsForValue().set(key, value, ttl)
    }
    
    fun set(key: String, value: Any, ttl: Long, timeUnit: TimeUnit) {
        redisTemplate.opsForValue().set(key, value, ttl, timeUnit)
    }
    
    fun get(key: String): Any? {
        return redisTemplate.opsForValue().get(key)
    }
    
    fun get(key: String, type: Class<T>): T? {
        val value = redisTemplate.opsForValue().get(key)
        return if (type.isInstance(value)) {
            type.cast(value)
        } else {
            null
        }
    }
    
    fun delete(key: String): Boolean {
        return redisTemplate.delete(key) ?: false
    }
    
    fun exists(key: String): Boolean {
        return redisTemplate.hasKey(key) ?: false
    }
    
    fun expire(key: String, ttl: Duration): Boolean {
        return redisTemplate.expire(key, ttl) ?: false
    }
    
    fun expire(key: String, ttl: Long, timeUnit: TimeUnit): Boolean {
        return redisTemplate.expire(key, ttl, timeUnit) ?: false
    }
    
    fun getTtl(key: String): Duration? {
        val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
        return if (ttl > 0) Duration.ofSeconds(ttl) else null
    }
    
    fun increment(key: String, delta: Long = 1): Long {
        return redisTemplate.opsForValue().increment(key, delta) ?: 0
    }
    
    fun decrement(key: String, delta: Long = 1): Long {
        return redisTemplate.opsForValue().increment(key, -delta) ?: 0
    }
    
    fun setIfAbsent(key: String, value: Any, ttl: Duration = Duration.ofHours(1)): Boolean {
        val result = redisTemplate.opsForValue().setIfAbsent(key, value)
        if (result == true) {
            redisTemplate.expire(key, ttl)
        }
        return result ?: false
    }
    
    fun setIfAbsent(key: String, value: Any, ttl: Long, timeUnit: TimeUnit): Boolean {
        val result = redisTemplate.opsForValue().setIfAbsent(key, value)
        if (result == true) {
            redisTemplate.expire(key, ttl, timeUnit)
        }
        return result ?: false
    }
    
    fun getAndSet(key: String, value: Any): Any? {
        return redisTemplate.opsForValue().getAndSet(key, value)
    }
    
    fun multiGet(keys: Collection<String>): List<Any?> {
        return redisTemplate.opsForValue().multiGet(keys) ?: emptyList()
    }
    
    fun multiSet(map: Map<String, Any>) {
        redisTemplate.opsForValue().multiSet(map)
    }
    
    fun multiSetIfAbsent(map: Map<String, Any>): Boolean {
        return redisTemplate.opsForValue().multiSetIfAbsent(map) ?: false
    }
    
    fun size(key: String): Long {
        return redisTemplate.opsForValue().size(key) ?: 0
    }
    
    fun append(key: String, value: String): Int {
        return redisTemplate.opsForValue().append(key, value) ?: 0
    }
    
    fun getRange(key: String, start: Long, end: Long): String {
        return redisTemplate.opsForValue().get(key, start, end) ?: ""
    }
    
    fun setRange(key: String, value: String, offset: Long): Long {
        return redisTemplate.opsForValue().set(key, value, offset) ?: 0
    }
}
