package com.toss.auth.controller

import com.toss.auth.dto.*
import com.toss.auth.service.AuthService
import com.toss.shared.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {
    
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<RefreshTokenResponse>> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
    
    @PostMapping("/logout")
    fun logout(
        authentication: Authentication,
        @RequestBody(required = false) request: Map<String, String>?
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = authentication.name.toLong()
        val refreshToken = request?.get("refreshToken")
        authService.logout(userId, refreshToken)
        return ResponseEntity.ok(ApiResponse.success())
    }
    
    @PostMapping("/change-password")
    fun changePassword(
        authentication: Authentication,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val userId = authentication.name.toLong()
        authService.changePassword(userId, request)
        return ResponseEntity.ok(ApiResponse.success())
    }
    
    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<ApiResponse<UserResponse>> {
        val userId = authentication.name.toLong()
        val user = authService.getUserInfo(userId)
        return ResponseEntity.ok(ApiResponse.success(user))
    }
}
