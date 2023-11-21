package com.example.kotlinproject.post

import com.example.kotlinproject.auth.Auth
import com.example.kotlinproject.auth.AuthProfile
import com.example.kotlinproject.post.Post.backgroundimage
import com.example.kotlinproject.post.Post.content
import com.example.kotlinproject.post.Post.latitude
import com.example.kotlinproject.post.Post.title
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/community")
class PostController(private  val resourceLoader: ResourceLoader) {
    private val POST_FILE_PATH = "post/files"
    val currentTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")



    @GetMapping("/posts")
    fun fetchPosts(): ResponseEntity<List<PostListResponse>> = transaction {
        val posts = Post.selectAll() .map { post ->
            val postId = post[Post.id]
            val files = PostFiles.select { (PostFiles.postId eq postId)
            }
                .map { file ->
                    PostFileResponse(
                        id = file[PostFiles.id].value,
                        postId = postId,
                        uuidFileName = file[PostFiles.uuidFileName],
                        originalFileName = file[PostFiles.originalFileName],
                        contentType = file[PostFiles.contentType]
                    )
                }

            PostListResponse(
                id = postId,
                userid = post[Post.userId],
                nickname = post[Post.nickname],
                title = post[Post.title],
                createdDate = post[Post.createdDate].toString(),
                files = files,
                latitude = post[Post.latitude],
                longitude = post[Post.longitude],

                )
        }
        ResponseEntity.ok(posts)
    }
    @Auth
    @GetMapping("/detail/{postId}")
    fun fetchPostDetail(@PathVariable postId: Long, @RequestAttribute authProfile: AuthProfile
                       ): ResponseEntity<PostWithFileResponse> = transaction {
        val post = Post.select { Post.id eq postId }.singleOrNull() ?: return@transaction ResponseEntity.notFound().build()

        val files = PostFiles.select { PostFiles.postId eq postId }
            .map { file ->
                PostFileResponse(
                    id = file[PostFiles.id].value,
                    postId = postId,
                    uuidFileName = file[PostFiles.uuidFileName],
                    originalFileName = file[PostFiles.originalFileName],
                    contentType = file[PostFiles.contentType]
                )
            }

        val postResponse = PostWithFileResponse(
            id = postId,
            title = post[Post.title],
            content = post[Post.content],
            createdDate = post[Post.createdDate].toString(),
            files = files,
            backgroundImage = post[Post.backgroundimage].toString(),
            latitude = post[Post.latitude],
            longitude = post[Post.longitude],

            )

        ResponseEntity.ok(postResponse)
    }

    @Auth
    @DeleteMapping("/delete/{postId}")
    fun deletePost(@RequestAttribute authProfile: AuthProfile,
                   @PathVariable postId: Long) : ResponseEntity<Map<String,Any>>{
        val reseult = transaction {
            PostFiles.select {
                (PostFiles.postId eq postId)
            }
            PostFiles.deleteWhere {
                (PostFiles.postId eq postId)
            }
        }

        val response = transaction {
            Post.select {
                (Post.id eq postId)
            }
            Post.deleteWhere {
                (Post.id eq postId)
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND).body(mapOf("data" to reseult))


    }
    @Auth
    @PutMapping("/update/{postId}")
    fun updatePost(
        @RequestAttribute authProfile: AuthProfile,
        @PathVariable postId: Long,
        @RequestBody updateRequest: UpdatePostRequest
    ): ResponseEntity<Any> {
        val updatedPost = transaction {
            Post.select { Post.id eq postId }
                .singleOrNull() ?: return@transaction ResponseEntity.notFound().build<Post>()

            Post.update({ Post.id eq postId }) {
                if (updateRequest.title != null) it[title] = updateRequest.title
                if (updateRequest.content != null) it[content] = updateRequest.content
            }

            Post.select { Post.id eq postId }.single()
        }

        return ResponseEntity.ok(updatedPost)
    }

    @Auth
    @PostMapping("/write")
    fun postWrite (
        @RequestBody postRequest: PostRequest,
        @RequestAttribute authProfile: AuthProfile
    ){
        transaction {
            Post.insert {
                it[title] = postRequest.title
                it[content] = postRequest.content
                it[userId] = authProfile.userLoginId
                it[nickname] = authProfile.nickname
                it[createdDate] = currentTime.format((formatter))
                it[backgroundimage] = postRequest.backgroundImage
            }
        }
    }
    @Auth
    @PostMapping("/with-file") // 색인
    fun writeFile(
        @RequestAttribute authProfile: AuthProfile, // *
        @RequestParam files: Array<MultipartFile>,
        @RequestParam title: String,
        @RequestParam content: String,
        @RequestParam latitude: Double?,
        @RequestParam longitude: Double?,
        @RequestParam backgroundImage: String?)

            : ResponseEntity<PostWithFileResponse> {
        println("title: $title")
        println("content: $content")
        println("backgroundImage: $backgroundImage")

        val dirPath = Paths.get(POST_FILE_PATH)
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath)
        }

        val filesList = mutableListOf<Map<String, String?>>()

        runBlocking {
            files.forEach {
                launch {
                    println("filename: ${it.originalFilename}")

                    val uuidFileName =
                        buildString {
                            append(UUID.randomUUID().toString())
                            append(".")
                            append(it.originalFilename!!.split(".").last())
                        }
                    val filePath = dirPath.resolve(uuidFileName)

                    it.inputStream.use {
                        Files.copy(it, filePath, StandardCopyOption.REPLACE_EXISTING)
                    }

                    filesList.add(
                        mapOf(
                            "uuidFileName" to uuidFileName,
                            "contentType" to it.contentType,
                            "originalFileName" to it.originalFilename))
                }
                print(filesList)
            }
        }

        val result = transaction {
            val p = Post
            val pf = PostFiles

            val insertedPost = p.insert {
                it[this.title] = title
                it[this.userId] = authProfile.userLoginId
                it[this.nickname] = authProfile.nickname
                it[this.content] = content
                it[this.createdDate] = currentTime.format((formatter))
                it[this.backgroundimage] = backgroundImage
                it[this.latitude] = latitude
                it[this.longitude] = longitude
            }.resultedValues!!.first()

            pf.batchInsert(filesList) {
                this[pf.postId] = insertedPost[p.id]
                this[pf.contentType] = it["contentType"] as String
                this[pf.originalFileName] = it["originalFileName"] as String
                this[pf.uuidFileName] = it["uuidFileName"] as String
            }
            val insertedPostFiles = pf.select { pf.postId eq insertedPost[p.id] }.map { r ->
                PostFileResponse(
                    id = r[pf.id].value,
                    postId = insertedPost[p.id],
                    uuidFileName = r[pf.uuidFileName],
                    originalFileName = r[pf.originalFileName],
                    contentType = r[pf.contentType]                )
            }
            return@transaction PostWithFileResponse(
                id = insertedPost[p.id],
                title = title,
                content = content,
                createdDate = insertedPost[p.createdDate].toString(),
                files = insertedPostFiles,
                backgroundImage = insertedPost[p.backgroundimage],
                latitude = insertedPost[p.latitude],
                longitude = insertedPost[p.longitude],



                )
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/files")
    fun downloadFile(@RequestParam uuidFilename : String) : ResponseEntity<Any> {
        // 서버에서 해당 경로의 파일을 읽어오기
        println("check-------------------$uuidFilename")
        val file = Paths.get("$POST_FILE_PATH/$uuidFilename").toFile()
        // 파일이 없으면 not-found
        if (!file.exists()) {
            return ResponseEntity.notFound().build()
        }

        // 파일의 content-type 처리
        val mimeType = Files.probeContentType(file.toPath())
        val mediaType = MediaType.parseMediaType(mimeType)

        // file:files/post/dkdkdkd.png
        val resource = resourceLoader.getResource("file:$file")
        return ResponseEntity.ok()
            .contentType(mediaType) // video/mp4, image/png, image/jpeg
            .body(resource)
    }
}