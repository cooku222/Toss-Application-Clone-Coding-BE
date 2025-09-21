package com.toss.shared.security

import com.toss.shared.exception.ErrorCodes
import com.toss.shared.exception.TossException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractTokenFromRequest(request)
        
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                val userId = jwtUtil.getUserIdFromToken(token)
                val roles = jwtUtil.getRolesFromToken(token)
                val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
                
                val authentication = UsernamePasswordAuthenticationToken(
                    userId, null, authorities
                )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                throw TossException(
                    ErrorCodes.AUTH_TOKEN_INVALID,
                    "Invalid token",
                    HttpStatus.UNAUTHORIZED
                )
            }
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
