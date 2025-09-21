package com.toss.shared.security

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class RateLimitingService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    fun isAllowed(key: String, limit: Int, window: Duration): Boolean {
        val now = LocalDateTime.now()
        val windowStart = now.minus(window)
        
        val script = """
            local key = KEYS[1]
            local window_start = ARGV[1]
            local limit = tonumber(ARGV[2])
            local now = ARGV[3]
            
            -- Remove expired entries
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
            
            -- Count current entries
            local current = redis.call('ZCARD', key)
            
            if current < limit then
                -- Add current request
                redis.call('ZADD', key, now, now)
                redis.call('EXPIRE', key, 3600) -- Expire after 1 hour
                return 1
            else
                return 0
            end
        """.trimIndent()
        
        val result = redisTemplate.execute { connection ->
            connection.eval(
                script.toByteArray(),
                1,
                key.toByteArray(),
                windowStart.toString().toByteArray(),
                limit.toString().toByteArray(),
                now.toString().toByteArray()
            )
        }
        
        return result == 1L
    }
    
    fun getRemainingRequests(key: String, limit: Int, window: Duration): Int {
        val now = LocalDateTime.now()
        val windowStart = now.minus(window)
        
        val script = """
            local key = KEYS[1]
            local window_start = ARGV[1]
            local limit = tonumber(ARGV[2])
            
            -- Remove expired entries
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
            
            -- Count current entries
            local current = redis.call('ZCARD', key)
            
            return limit - current
        """.trimIndent()
        
        val result = redisTemplate.execute { connection ->
            connection.eval(
                script.toByteArray(),
                1,
                key.toByteArray(),
                windowStart.toString().toByteArray(),
                limit.toString().toByteArray()
            )
        }
        
        return (result as? Long)?.toInt() ?: 0
    }
    
    fun getResetTime(key: String, window: Duration): LocalDateTime {
        val now = LocalDateTime.now()
        val windowStart = now.minus(window)
        
        val script = """
            local key = KEYS[1]
            local window_start = ARGV[1]
            
            -- Remove expired entries
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
            
            -- Get oldest entry
            local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
            
            if #oldest > 0 then
                return oldest[2]
            else
                return 0
            end
        """.trimIndent()
        
        val result = redisTemplate.execute { connection ->
            connection.eval(
                script.toByteArray(),
                1,
                key.toByteArray(),
                windowStart.toString().toByteArray()
            )
        }
        
        return if (result != null && result != 0L) {
            LocalDateTime.parse(result.toString())
        } else {
            now.plus(window)
        }
    }
    
    fun isAllowedByUser(userId: Long, endpoint: String, limit: Int, window: Duration): Boolean {
        val key = "rate_limit:user:$userId:$endpoint"
        return isAllowed(key, limit, window)
    }
    
    fun isAllowedByIp(ip: String, endpoint: String, limit: Int, window: Duration): Boolean {
        val key = "rate_limit:ip:$ip:$endpoint"
        return isAllowed(key, limit, window)
    }
    
    fun isAllowedByApiKey(apiKey: String, endpoint: String, limit: Int, window: Duration): Boolean {
        val key = "rate_limit:api_key:$apiKey:$endpoint"
        return isAllowed(key, limit, window)
    }
    
    fun getRateLimitInfo(key: String, limit: Int, window: Duration): RateLimitInfo {
        val remaining = getRemainingRequests(key, limit, window)
        val resetTime = getResetTime(key, window)
        
        return RateLimitInfo(
            limit = limit,
            remaining = remaining,
            resetTime = resetTime,
            window = window
        )
    }
}

data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val resetTime: LocalDateTime,
    val window: Duration
)
