package com.example.kotlinproject.auth.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.example.kotlinproject.auth.AuthProfile
import java.util.*

object JwtUtil {
    var secret = "werojw@eoirjew1313"

    val TOKEN_TIMEOUT = (1000 * 60 * 60 * 24 * 7).toLong()

    fun createToken(id: Long, userid: String, username: String, nickname : String): String? {
        val now = Date()
        val exp = Date(now.time+ TOKEN_TIMEOUT)
        val algorithm = Algorithm.HMAC256(secret)
        return JWT.create()
            .withSubject(id.toString())
            .withClaim("userid",userid)
            .withClaim("username",username)
            .withClaim("nickname",nickname)
            .withIssuedAt(now)
            .withExpiresAt(exp)
            .sign(algorithm)
    }

    fun validateToken(token : String) : AuthProfile? {
        val algorithm = Algorithm.HMAC256(secret)
        val verifier: JWTVerifier = JWT.require(algorithm).build()

        return try {
            val decodedJWT: DecodedJWT = verifier.verify(token)
            val id: Long = java.lang.Long.valueOf(decodedJWT.getSubject())
            val username: String = decodedJWT
                .getClaim("username").asString()
            val userid : String = decodedJWT
                .getClaim("userid").asString()
            val nickname : String = decodedJWT
                .getClaim("nickname").asString()

            AuthProfile(id, userid,username, nickname)
        } catch (e: JWTVerificationException) {
            // 토큰 검증 오류 상황
            null
        }

    }
}