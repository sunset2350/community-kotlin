package com.example.kotlinproject.auth


import com.example.kotlinproject.auth.util.HashUtil
import com.example.kotlinproject.auth.util.JwtUtil
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Service


@Service
class AuthService(private val database: Database) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    fun createIdentity(req: SignupRequest): Long {
        val record = transaction {
            Identities
                .select {
                    Identities.userLoginId eq req.userid
                }.singleOrNull()
        }

        if (record != null) {
            return 0;
        }
        val secret = HashUtil.createHash(req.userpassword)

        val profileId = transaction {
            try {

                val identityId = Identities.insertAndGetId {
                    it[this.userLoginId] = req.userid
                    it[this.secret] = secret
                }

                val profileId = Profiles.insertAndGetId {
                    it[this.username] = req.username
                    it[this.nickname] = req.nickname
                    it[this.birth] = req.birth
                    it[this.sex] = req.usersex
                    it[this.userLoginId] = req.userid
                    it[this.image] = ""
                    it[this.introduction] = ""


                }

                return@transaction profileId.value
            } catch (e: Exception) {
                logger.error(e.message)
                rollback()
                return@transaction 0
            }
        }
        return profileId


    }

    fun authenticate(userLoginId: String, userPassword: String): Pair<Boolean, String?> {
        val (result, payload) = transaction(
            database.transactionManager.defaultIsolationLevel,
            readOnly = true
        ) {
            val i = Identities; // 테이블네임 별칭(alias) 단축해서 쓸 수 있음
            val p = Profiles;

            // 인증정보 조회
            val identityRecord = i.select { i.userLoginId eq userLoginId }.singleOrNull()
                ?: return@transaction Pair(false, mapOf("message" to "Unauthorized"))

            // 프로필정보 조회
            val profileRecord = p.select { p.userLoginId eq userLoginId }.singleOrNull()
                ?: return@transaction Pair(false, mapOf("message" to "Conflict"))

            return@transaction Pair(
                true, mapOf(
                    "secret" to identityRecord[i.secret],
                    "userid" to identityRecord[i.userLoginId],
                    "id" to profileRecord[p.id],
                    "nickname" to profileRecord[p.nickname],
                    "username" to profileRecord[p.username],
                    "birth" to profileRecord[p.birth],
                    "sex" to profileRecord[p.sex]
                )
            )
        }

        if (!result) {
            return Pair(false, payload["message"].toString());

        }

        val isVerified = HashUtil.verifyHash(userPassword, payload["secret"].toString())
        if (!isVerified) {
            return Pair(false, "Unauthorized")

        }
        val token = JwtUtil.createToken(
            payload["id"].toString().toLong(),
            payload["userid"].toString(),
            payload["username"].toString(),
            payload["nickname"].toString()
        )

        return Pair(true, token)
    }
}