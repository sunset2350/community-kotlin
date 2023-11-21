package com.example.kotlinproject.post

import com.example.kotlinproject.auth.Profiles
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.doubleLiteral
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
object Post : Table("post_table") {
    val id = long("post_id").autoIncrement()
    val userId = varchar("user_id",30)
    val nickname = varchar("nickname",30)
    val title = varchar("post_title",30)
    val content = text("post_content")
    val createdDate = varchar("created_date",30)
    val backgroundimage = varchar("background_image",255).nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()


    override val primaryKey = PrimaryKey(id, name = "pk_post_id")

}
object PostFiles : LongIdTable("post_file"){
    val postId = reference("post_id", Post.id)
    val originalFileName = varchar("original_file_name", 200)
    val uuidFileName = varchar("uuid", 50).uniqueIndex()
    val contentType = varchar("content_type",  100)
}