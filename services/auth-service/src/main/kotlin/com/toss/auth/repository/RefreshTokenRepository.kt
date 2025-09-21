package com.toss.auth.repository

import com.toss.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    
    fun findByToken(token: String): RefreshToken?
    
    fun findByUserIdAndIsRevokedFalse(userId: Long): List<RefreshToken>
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :revokedAt, rt.revokedBy = :revokedBy WHERE rt.userId = :userId")
    fun revokeAllByUserId(@Param("userId") userId: Long, @Param("revokedAt") revokedAt: LocalDateTime, @Param("revokedBy") revokedBy: String)
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :revokedAt, rt.revokedBy = :revokedBy WHERE rt.token = :token")
    fun revokeByToken(@Param("token") token: String, @Param("revokedAt") revokedAt: LocalDateTime, @Param("revokedBy") revokedBy: String)
    
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now AND rt.isRevoked = false")
    fun findExpiredTokens(@Param("now") now: LocalDateTime): List<RefreshToken>
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    fun deleteExpiredTokens(@Param("now") now: LocalDateTime)
}
