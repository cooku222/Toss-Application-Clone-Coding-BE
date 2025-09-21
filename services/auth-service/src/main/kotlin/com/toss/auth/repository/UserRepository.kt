package com.toss.auth.repository

import com.toss.auth.entity.User
import com.toss.auth.entity.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    fun findByEmail(email: String): User?
    
    fun findByPhoneNumber(phoneNumber: String): User?
    
    fun findByEmailAndStatus(email: String, status: UserStatus): User?
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status AND (u.lockedUntil IS NULL OR u.lockedUntil < :now)")
    fun findActiveUserByEmail(@Param("email") email: String, @Param("status") status: UserStatus, @Param("now") now: LocalDateTime): User?
    
    fun existsByEmail(email: String): Boolean
    
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    fun countByStatus(@Param("status") status: UserStatus): Long
}
