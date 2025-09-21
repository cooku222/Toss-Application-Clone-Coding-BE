package com.toss.shared.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*
import java.util.concurrent.TimeUnit

@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiration:3600}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration:604800}") private val refreshTokenExpiration: Long
) {
    
    private val key: Key = Keys.hmacShaKeyFor(secret.toByteArray())
    
    fun generateAccessToken(userId: String, roles: List<String>): String {
        val now = Date()
        val expiryDate = Date(now.time + TimeUnit.SECONDS.toMillis(accessTokenExpiration))
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("roles", roles)
            .claim("type", "access")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
    
    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiryDate = Date(now.time + TimeUnit.SECONDS.toMillis(refreshTokenExpiration))
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
    
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: JwtException) {
            false
        }
    }
    
    fun getUserIdFromToken(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        
        return claims.subject
    }
    
    fun getRolesFromToken(token: String): List<String> {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        
        @Suppress("UNCHECKED_CAST")
        return claims["roles"] as? List<String> ?: emptyList()
    }
    
    fun getTokenType(token: String): String? {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        
        return claims["type"] as? String
    }
    
    fun isTokenExpired(token: String): Boolean {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        
        return claims.expiration.before(Date())
    }
    
    fun getExpirationFromToken(token: String): Date {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        
        return claims.expiration
    }
}
