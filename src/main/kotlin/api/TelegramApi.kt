package api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.io.File

class TelegramApi(token: String) {

    private val baseUrl = "https://api.telegram.org/bot$token"

    private val socksServer = System.getenv("SOCKS_SERVER")
    private val socksPort = System.getenv("SOCKS_PORT")?.toIntOrNull()

    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 60_000
        }
        engine {
            if (!socksServer.isNullOrBlank() && socksPort != null) {
                proxy = ProxyBuilder.socks(socksServer, socksPort)
            }
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
