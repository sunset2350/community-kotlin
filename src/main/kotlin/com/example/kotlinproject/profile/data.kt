package com.example.kotlinproject.profile

data class ProfileResponse(
    val id: String,
    val userLoginId: String,
    val username: String,
    val sex: String,
    val nickname: String,
    val birth: String,
    val introduction: String,
    val image: String
)