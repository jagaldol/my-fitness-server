package com.jagaldol.myfitness._core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter


class JwtAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val jwt = request.getHeader(JwtProvider.HEADER)

        jwt?.let {
            try {
                val decodedJWT = JwtProvider.verify(jwt, JwtProvider.TYPE_ACCESS)
                val id = decodedJWT.subject.toLong()

                val userDetails = CustomUserDetails(userId = id)

                val authentication: Authentication = UsernamePasswordAuthenticationToken(
                        userDetails,
                        userDetails.password,
                        userDetails.authorities
                )
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
//                TODO("Not yet implemented")
            }
        }

        chain.doFilter(request, response)
    }


}