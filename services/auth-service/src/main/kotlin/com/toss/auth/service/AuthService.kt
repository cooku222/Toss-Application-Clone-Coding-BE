package com.toss.auth.service

import com.toss.auth.dto.*
import com.toss.auth.entity.RefreshToken
import com.toss.auth.entity.User
import com.toss.auth.entity.UserRole
import com.toss.auth.entity.UserStatus
import com.toss.auth.repository.RefreshTokenRepository
import com.toss.auth.repository.UserRepository
import com.toss.shared.exception.ErrorCodes
import com.toss.shared.exception.TossException
import com.toss.shared.security.JwtUtil
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    fun register(request: RegisterRequest): AuthResponse {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email)) {
            throw TossException(ErrorCodes.AUTH_USER_NOT_FOUND, "User already exists with this email")
        }
        
        if (userRepository.existsByPhoneNumber(request.phoneNumber)) {
            throw TossException(ErrorCodes.AUTH_USER_NOT_FOUND, "User already exists with this phone number")
        }
        
        // Create new user
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            phoneNumber = request.phoneNumber,
            status = UserStatus.ACTIVE,
            roles = setOf(UserRole.USER)
        )
        
        val savedUser = userRepository.save(user)
        
        // Generate tokens
        val accessToken = jwtUtil.generateAccessToken(savedUser.id.toString(), savedUser.roles.map { it.name })
        val refreshToken = jwtUtil.generateRefreshToken(savedUser.id.toString())
        
        // Save refresh token
        val refreshTokenEntity = RefreshToken(
            token = refreshToken,
            userId = savedUser.id,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        refreshTokenRepository.save(refreshTokenEntity)
        
        // Cache user info
        cacheUserInfo(savedUser)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600,
            user = savedUser.toUserInfo()
        )
    }
    
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findActiveUserByEmail(
            request.email, 
            UserStatus.ACTIVE, 
            LocalDateTime.now()
        ) ?: throw TossException(ErrorCodes.AUTH_USER_NOT_FOUND, "Invalid credentials")
        
        // Check password
        if (!passwordEncoder.matches(request.password, user.password)) {
            handleFailedLogin(user)
            throw TossException(ErrorCodes.AUTH_PASSWORD_INCORRECT, "Invalid credentials")
        }
        
        // Check if account is locked
        if (user.lockedUntil != null && user.lockedUntil.isAfter(LocalDateTime.now())) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "Account is locked")
        }
        
        // Reset failed login attempts
        resetFailedLoginAttempts(user)
        
        // Update last login
        user.copy(lastLoginAt = LocalDateTime.now())
        userRepository.save(user)
        
        // Generate tokens
        val accessToken = jwtUtil.generateAccessToken(user.id.toString(), user.roles.map { it.name })
        val refreshToken = jwtUtil.generateRefreshToken(user.id.toString())
        
        // Save refresh token
        val refreshTokenEntity = RefreshToken(
            token = refreshToken,
            userId = user.id,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        refreshTokenRepository.save(refreshTokenEntity)
        
        // Cache user info
        cacheUserInfo(user)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600,
            user = user.toUserInfo()
        )
    }
    
    fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw TossException(ErrorCodes.AUTH_TOKEN_INVALID, "Invalid refresh token")
        
        if (refreshTokenEntity.isRevoked || refreshTokenEntity.expiresAt.isBefore(LocalDateTime.now())) {
            throw TossException(ErrorCodes.AUTH_TOKEN_EXPIRED, "Refresh token expired")
        }
        
        val user = userRepository.findById(refreshTokenEntity.userId)
            .orElseThrow { TossException(ErrorCodes.AUTH_USER_NOT_FOUND, "User not found") }
        
        if (user.status != UserStatus.ACTIVE) {
            throw TossException(ErrorCodes.AUTH_ACCESS_DENIED, "User account is not active")
        }
        
        // Generate new tokens
        val newAccessToken = jwtUtil.generateAccessToken(user.id.toString(), user.roles.map { it.name })
        val newRefreshToken = jwtUtil.generateRefreshToken(user.id.toString())
        
        // Revoke old refresh token
        refreshTokenRepository.revokeByToken(
            request.refreshToken, 
            LocalDateTime.now(), 
            "refresh_rotation"
        )
        
        // Save new refresh token
        val newRefreshTokenEntity = RefreshToken(
            token = newRefreshToken,
            userId = user.id,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        refreshTokenRepository.save(newRefreshTokenEntity)
        
        return RefreshTokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = 3600
        )
    }
    
    fun logout(userId: Long, refreshToken: String?) {
        // Revoke all refresh tokens for user
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now(), "logout")
        
        // Remove from cache
        redisTemplate.delete("user:$userId")
        
        // If specific refresh token provided, revoke it
        refreshToken?.let {
            refreshTokenRepository.revokeByToken(it, LocalDateTime.now(), "logout")
        }
    }
    
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { TossException(ErrorCodes.AUTH_USER_NOT_FOUND, "User not found") }
        
        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw TossException(ErrorCodes.AUTH_PASSWORD_INCORRECT, "Current password is incorrect")
        }
        
        val updatedUser = user.copy(password = passwordEncoder.encode(request.newPassword))
        userRepository.save(updatedUser)
        
        // Revoke all refresh tokens to force re-login
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now(), "password_change")
    }
    
    fun getUserInfo(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { TossException(ErrorCodes.AUTH_USER_NOT_FOUND, "User not found") }
        
        return user.toUserResponse()
    }
    
    private fun handleFailedLogin(user: User) {
        val failedAttempts = user.failedLoginAttempts + 1
        val lockedUntil = if (failedAttempts >= 5) {
            LocalDateTime.now().plusMinutes(30) // Lock for 30 minutes
        } else {
            null
        }
        
        val updatedUser = user.copy(
            failedLoginAttempts = failedAttempts,
            lockedUntil = lockedUntil
        )
        userRepository.save(updatedUser)
    }
    
    private fun resetFailedLoginAttempts(user: User) {
        if (user.failedLoginAttempts > 0) {
            val updatedUser = user.copy(
                failedLoginAttempts = 0,
                lockedUntil = null
            )
            userRepository.save(updatedUser)
        }
    }
    
    private fun cacheUserInfo(user: User) {
        val key = "user:${user.id}"
        val userInfo = user.toUserInfo()
        // Cache for 1 hour
        redisTemplate.opsForValue().set(key, userInfo.toString(), 1, TimeUnit.HOURS)
    }
}
