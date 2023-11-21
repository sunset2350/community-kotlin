package com.example.kotlinproject.auth

import com.example.kotlinproject.auth.util.JwtUtil
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/auth")
class AuthController(private val service: AuthService) {


    @PostMapping("/sign")
    fun signUp(@RequestBody req: SignupRequest): ResponseEntity<Long> {
        val profileId = service.createIdentity(req)
        if (profileId > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body(profileId)

        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(profileId)
        }
    }

    @PostMapping("/signin")
    fun signIn(
        @RequestParam userLoginId: String,
        @RequestParam userPassword: String,
        res: HttpServletResponse,
    ): ResponseEntity<*> {

        val (result, message) = service.authenticate(userLoginId, userPassword)


        if (result) {
            val cookie = Cookie("token", message)
            cookie.path = "/"
            cookie.maxAge = (JwtUtil.TOKEN_TIMEOUT / 1000L).toInt()
            cookie.domain = "localhost"

            res.addCookie(cookie)

            return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(
                    ServletUriComponentsBuilder
                        .fromHttpUrl("http://localhost:5000/")
                        .build().toUri()
                )

                .build<Any>()

        }
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(
                ServletUriComponentsBuilder
                    .fromHttpUrl("http://localhost:5000/login.html?err=$message")
                    .build()
                    .toUri()
            ).build<Any>()


    }

}