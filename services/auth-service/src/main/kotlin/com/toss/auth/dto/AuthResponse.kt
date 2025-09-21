package com.toss.auth.dto

import com.toss.auth.entity.User
import java.time.LocalDateTime

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserInfo
)

data class UserInfo(
    val id: Long,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val roles: List<String>,
    val lastLoginAt: LocalDateTime?
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val status: String,
    val roles: List<String>,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime?
)

// Extension functions for mapping
fun User.toUserInfo(): UserInfo {
    return UserInfo(
        id = this.id,
        email = this.email,
        name = this.name,
        phoneNumber = this.phoneNumber,
        roles = this.roles.map { it.name },
        lastLoginAt = this.lastLoginAt
    )
}

fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id,
        email = this.email,
        name = this.name,
        phoneNumber = this.phoneNumber,
        status = this.status.name,
        roles = this.roles.map { it.name },
        createdAt = this.createdAt,
        lastLoginAt = this.lastLoginAt
    )
}
