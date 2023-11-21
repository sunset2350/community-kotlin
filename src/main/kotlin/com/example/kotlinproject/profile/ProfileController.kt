package com.example.kotlinproject.profile

import com.example.kotlinproject.auth.Auth
import com.example.kotlinproject.auth.AuthProfile
import com.example.kotlinproject.auth.Profiles
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Connection

@RestController
@RequestMapping("/user")

class ProfileController {


    @Auth
    @GetMapping("/profile")
    fun showProfile(@RequestAttribute authProfile: AuthProfile): List<ProfileResponse> =
        transaction(Connection.TRANSACTION_READ_UNCOMMITTED, readOnly = false) {
            val query = when {
                authProfile.userLoginId != null -> Profiles.select {
                    (Profiles.id eq authProfile.id)
                }

                else -> Profiles.select {
                    (Profiles.id eq authProfile.id)
                }
            }

            query.map { r ->
                ProfileResponse(
                    r[Profiles.id].toString(),
                    r[Profiles.userLoginId],
                    r[Profiles.username],
                    r[Profiles.sex],
                    r[Profiles.nickname],
                    r[Profiles.birth],
                    r[Profiles.introduction],
                    r[Profiles.image]
                )
            }
        }

    @Auth
    @PutMapping("/{userid}")
    fun editProfile(
        @PathVariable userid: String,
        @RequestBody request: dataModify,
        @RequestAttribute authProfile: AuthProfile,

        ): ResponseEntity<Any> {
        transaction {
            Profiles.select {
                (Profiles.userLoginId eq userid) and (Profiles.id eq authProfile.id)
            }.firstOrNull()
        } ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()




        transaction {
            Profiles.update({ Profiles.userLoginId eq userid }) {
                it[username] = request.username
                it[nickname] = request.nickname
                it[sex] = request.sex
                it[birth] = request.birth
                it[introduction] = request.introduction
                it[image] = request.image
            }
        }
        return ResponseEntity.ok().build()
    }


}
