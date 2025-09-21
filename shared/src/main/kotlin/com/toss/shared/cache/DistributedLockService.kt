package com.toss.shared.cache

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

@Service
class DistributedLockService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    fun tryLock(lockKey: String, ttl: Duration = Duration.ofSeconds(30)): DistributedLock? {
        val lockValue = generateLockValue()
        val success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl)
        
        return if (success == true) {
            DistributedLock(lockKey, lockValue, redisTemplate, ttl)
        } else {
            null
        }
    }
    
    fun tryLock(lockKey: String, ttl: Long, timeUnit: TimeUnit): DistributedLock? {
        val lockValue = generateLockValue()
        val success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl, timeUnit)
        
        return if (success == true) {
            DistributedLock(lockKey, lockValue, redisTemplate, Duration.ofMillis(timeUnit.toMillis(ttl)))
        } else {
            null
        }
    }
    
    fun lock(lockKey: String, ttl: Duration = Duration.ofSeconds(30), maxWaitTime: Duration = Duration.ofSeconds(10)): DistributedLock {
        val startTime = System.currentTimeMillis()
        val maxWaitMillis = maxWaitTime.toMillis()
        
        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            val lock = tryLock(lockKey, ttl)
            if (lock != null) {
                return lock
            }
            
            try {
                Thread.sleep(100) // Wait 100ms before retry
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw RuntimeException("Lock acquisition interrupted", e)
            }
        }
        
        throw RuntimeException("Failed to acquire lock within timeout: $lockKey")
    }
    
    fun lock(lockKey: String, ttl: Long, timeUnit: TimeUnit, maxWaitTime: Long, maxWaitTimeUnit: TimeUnit): DistributedLock {
        return lock(
            lockKey, 
            Duration.ofMillis(timeUnit.toMillis(ttl)), 
            Duration.ofMillis(maxWaitTimeUnit.toMillis(maxWaitTime))
        )
    }
    
    fun executeWithLock(lockKey: String, ttl: Duration = Duration.ofSeconds(30), action: () -> Unit) {
        val lock = lock(lockKey, ttl)
        try {
            action()
        } finally {
            lock.unlock()
        }
    }
    
    fun executeWithLock(lockKey: String, ttl: Long, timeUnit: TimeUnit, action: () -> Unit) {
        executeWithLock(lockKey, Duration.ofMillis(timeUnit.toMillis(ttl)), action)
    }
    
    fun tryExecuteWithLock(lockKey: String, ttl: Duration = Duration.ofSeconds(30), action: () -> Unit): Boolean {
        val lock = tryLock(lockKey, ttl)
        return if (lock != null) {
            try {
                action()
                true
            } finally {
                lock.unlock()
            }
        } else {
            false
        }
    }
    
    fun tryExecuteWithLock(lockKey: String, ttl: Long, timeUnit: TimeUnit, action: () -> Unit): Boolean {
        return tryExecuteWithLock(lockKey, Duration.ofMillis(timeUnit.toMillis(ttl)), action)
    }
    
    private fun generateLockValue(): String {
        return "${Thread.currentThread().id}-${System.currentTimeMillis()}-${(Math.random() * 1000000).toInt()}"
    }
}

class DistributedLock(
    private val lockKey: String,
    private val lockValue: String,
    private val redisTemplate: RedisTemplate<String, String>,
    private val ttl: Duration
) : Lock {
    
    private var isLocked = true
    
    override fun lock() {
        // Already locked in constructor
    }
    
    override fun lockInterruptibly() {
        lock()
    }
    
    override fun tryLock(): Boolean {
        return isLocked
    }
    
    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        return isLocked
    }
    
    override fun unlock() {
        if (isLocked) {
            // Use Lua script to ensure atomic unlock
            val script = """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
            """.trimIndent()
            
            redisTemplate.execute { connection ->
                connection.eval(script.toByteArray(), 1, lockKey.toByteArray(), lockValue.toByteArray())
            }
            
            isLocked = false
        }
    }
    
    override fun newCondition() = throw UnsupportedOperationException("Conditions not supported")
    
    fun extend(additionalTtl: Duration) {
        if (isLocked) {
            val script = """
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("expire", KEYS[1], ARGV[2])
                else
                    return 0
                end
            """.trimIndent()
            
            redisTemplate.execute { connection ->
                connection.eval(
                    script.toByteArray(), 
                    1, 
                    lockKey.toByteArray(), 
                    lockValue.toByteArray(),
                    additionalTtl.seconds.toString().toByteArray()
                )
            }
        }
    }
    
    fun isLocked(): Boolean = isLocked
    
    fun getRemainingTtl(): Duration? {
        if (!isLocked) return null
        
        val ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)
        return if (ttl > 0) Duration.ofSeconds(ttl) else null
    }
}
