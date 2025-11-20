package api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class TgUpdate(val update_id: Long, val message: TgMessage? = null)

@Serializable
data class TgMessage(val message_id: Long, val chat: TgChat, val text: String? = null)

@Serializable
data class TgChat(val id: Long)

class TelegramApi(private val token: String) {

    private val baseUrl = "https://api.telegram.org/bot$token"
    private val fileBase = "https://api.telegram.org/file/bot$token"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    suspend fun getUpdates(offset: Long?): List<TgUpdate> =
        client.get("$baseUrl/getUpdates") {
            offset?.let { parameter("offset", it) }
            parameter("timeout", 50)
        }.body()

    suspend fun sendMessage(chatId: Long, text: String) {
        client.post("$baseUrl/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("chat_id" to chatId, "text" to text))
        }
    }

    suspend fun sendDocument(chatId: Long, file: File, caption: String? = null) {
        client.submitFormWithBinaryData(
            url = "$baseUrl/sendDocument",
            formData = formData {
                append("chat_id", chatId.toString())
                append("document", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                })
                caption?.let { append("caption", it) }
            }
        )
    }
}
