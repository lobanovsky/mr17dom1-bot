package api

import RoomType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logger
import java.io.File

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
    val userId: Int,
    val workspaces: List<Int>
)

@Serializable
data class OverviewArea(
    val areaName: String? = null,
    val places: List<String>? = emptyList()
)

@Serializable
data class OverviewResponse(
    val carNumber: String,
    val carDescription: String? = null,
    val phoneNumber: String,
    val phoneLabel: String? = null,
    val tenant: Boolean? = null,
    val overviewAreas: List<OverviewArea> = emptyList(),
    val ownerName: String? = null,
    val ownerRooms: String? = null
)

@Serializable
data class AvailableMonthsResponse(val months: List<String>)

class HousekprApi(
    private val host: String,
    private val email: String,
    private val password: String
) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private var accessToken: String? = null

    /** Авторизация */
    suspend fun login(): Boolean {
        val response = client.post("$host/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        return if (response.status.isSuccess()) {
            val data: LoginResponse = response.body()
            accessToken = data.access_token.trim()
            logger().info("🔑 Новый токен получен (${accessToken!!.take(20)}...)")
            true
        } else {
            logger().info("❌ Ошибка авторизации: ${response.status}")
            false
        }
    }

    /** Запрос overview с автообновлением токена */
    suspend fun getOverview(carNumber: String): OverviewResponse? {
        if (accessToken == null) {
            if (!login()) return null
        }

        val result = makeOverviewRequest(carNumber)
        if (result == null) {
            logger().info("⚠️ Попробуем перелогиниться и повторить запрос...")
            if (login()) {
                return makeOverviewRequest(carNumber)
            }
        }
        return result
    }

    /** Реальный запрос (без логики обновления токена) */
    private suspend fun makeOverviewRequest(carNumber: String): OverviewResponse? {
        try {
            val response = client.get("$host/api/access/overview/$carNumber") {
                url { parameters.append("active", "true") }
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                accept(ContentType.Application.Json)
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                logger().info("❌ 401 Unauthorized — токен протух.")
                return null
            }

            if (!response.status.isSuccess()) {
                logger().info("❌ Ошибка API: ${response.status}")
                logger().info(response.bodyAsText())
                return null
            }

            return response.body()
        } catch (e: Exception) {
            logger().info("❌ Ошибка запроса: ${e.message}")
            return null
        }
    }

    suspend fun getAvailableMonths(): List<String> {
        if (accessToken == null) login()
        val response = client.get("$host/api/receipt/available-months") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            accept(ContentType.Application.Json)
        }
        return response.body<AvailableMonthsResponse>().months
    }

    suspend fun downloadReceiptPdf(
        year: String,
        month: String,
        roomType: RoomType,
        number: Int
    ): PdfFileData? {
        if (accessToken == null) login()

        return try {
            val response: HttpResponse = client.get("$host/api/receipt/merged") {
                url {
                    parameters.append("year", year)
                    parameters.append("month", month)
                    parameters.append("type", roomType.name)
                    parameters.append("number", number.toString())
                }
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.Accept, "application/pdf")
            }

            if (!response.status.isSuccess()) {
                logger().info("❌ Ошибка скачивания PDF: ${response.status}")
                return null
            }

            // ---- имя файла из Content-Disposition ----
            val cd = response.headers[HttpHeaders.ContentDisposition]
            val fileName = cd
                ?.substringAfter("filename=")
                ?.trim()
                ?.replace("\"", "")
                ?: "ЖКУ+Кап.ремонт ТСН \"МР17дом1\"-$year-$month-$roomType-$number.pdf"

            // ---- читаем байты ----
            val bytes = response.readRawBytes()

            PdfFileData(fileName, bytes)

        } catch (e: Exception) {
            logger().info("❌ Ошибка при скачивании PDF: ${e.message}")
            null
        }
    }
}

data class PdfFileData(
    val fileName: String,
    val bytes: ByteArray
)

fun PdfFileData.toTempFile(): File {
    val tempDir = System.getProperty("java.io.tmpdir")
    val file = File(tempDir, fileName)
    file.writeBytes(bytes)
    return file
}
