package com.example.kotlinproject.post


import org.bouncycastle.oer.its.ieee1609dot2.basetypes.KnownLatitude
import java.time.LocalDateTime

data class PostRequest(
    val title: String,
    val userid : String,
    val nickname : String,
    val content: String,
    val latitude: Double?,
    val longitude: Double?,
    val backgroundImage: String
)

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val createdDate: LocalDateTime
)

data class PostWithFileResponse(
    val id: Long,
    val title: String,
    val content: String,
    val createdDate: String,
    val files: List<PostFileResponse>,
    val backgroundImage: String?,
    val latitude: Double?,
    val longitude: Double?,

    )


data class PostFileResponse(
    val id: Long,
    val postId: Long,
    var uuidFileName: String,
    val originalFileName: String,
    val contentType: String,

    )

data class PostListResponse(
    val id: Long,
    val title: String,
    val userid: String,
    val nickname: String,
    val createdDate: String,
    val files: List<PostFileResponse>,
    val latitude: Double?,
    val longitude: Double?

)

data class UpdatePostRequest(
    val title: String?,
    val content: String?
)