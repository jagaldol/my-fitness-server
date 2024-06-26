package com.jagaldol.behind.fitness.user.controller

import com.jagaldol.behind.fitness._core.security.CustomUserDetails
import com.jagaldol.behind.fitness._core.security.JwtProvider
import com.jagaldol.behind.fitness._core.utils.ApiUtils
import com.jagaldol.behind.fitness.user.dto.UserRequest
import com.jagaldol.behind.fitness.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*


@RestController
class UserController(
    private val userService: UserService,
    private val jwtProvider: JwtProvider
) {
    @PostMapping("/login")
    fun login(
        request: HttpServletRequest,
        @RequestBody @Valid requestDto: UserRequest.LoginDto,
        errors: Errors
    ): ResponseEntity<ApiUtils.Response<Any?>> {
        val (access, refresh) = userService.login(requestDto, getIp(request))

        return ResponseEntity.ok().header(jwtProvider.header, access)
            .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(refresh, jwtProvider.refreshExp).toString())
            .body(ApiUtils.success())

    }

    @PostMapping("/authentication")
    fun reIssueTokens(
        request: HttpServletRequest,
        @CookieValue("refreshToken") refreshToken: String
    ): ResponseEntity<ApiUtils.Response<Any?>> {
        val (access, refresh) = userService.reIssueTokens(refreshToken, getIp(request))

        return ResponseEntity.ok().header(jwtProvider.header, access)
            .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(refresh, jwtProvider.refreshExp).toString())
            .body(ApiUtils.success())
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @CookieValue("refreshToken") refreshToken: String?
    ): ResponseEntity<ApiUtils.Response<Any?>> {
        userService.logout(userDetails.userId, refreshToken)
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie("", 0).toString())
            .body(ApiUtils.success())
    }


    private fun createRefreshTokenCookie(refreshToken: String, exp: Long) =
        ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true) // javascript 접근 방지
            .secure(true) // https 통신 강제
            .sameSite("None")
            .maxAge(exp)
            .build()

    private fun getIp(request: HttpServletRequest) =
        request.run {
            getHeader("X-Forwarded-For") ?: getHeader("Proxy-Client-IP") ?: getHeader("WL-Proxy-Client-IP") ?: getHeader("HTTP_CLIENT_IP")
            ?: getHeader("HTTP_X_FORWARDED_FOR") ?: remoteAddr
        }

    @GetMapping("/users/mine")
    fun getMyInfo(@AuthenticationPrincipal userDetails: CustomUserDetails) =
        ResponseEntity.ok().body(ApiUtils.success(userService.getMyInfo(userDetails.userId)))

    @PutMapping("users/mine")
    fun editMyInfo(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody @Valid requestDto: UserRequest.UpdateMyInfoDto, errors: Errors
    ): ResponseEntity<ApiUtils.Response<Any?>> {
        userService.updateMyInfo(userDetails.userId, requestDto)
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie("", 0).toString())
            .body(ApiUtils.success())
    }
}

