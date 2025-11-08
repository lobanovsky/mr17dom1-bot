import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    val userId: Long? = null
)

@Serializable
data class OverviewArea(
    val areaName: String,
    val places: List<String>
)

@Serializable
data class CarOverviewResponse(
    val carNumber: String? = null,
    val carDescription: String? = null,
    val phoneNumber: String? = null,
    val phoneLabel: String? = null,
    val tenant: Boolean? = null,
    val overviewAreas: List<OverviewArea> = emptyList(),
    val ownerName: String? = null,
    val ownerRooms: String? = null
)

class HousekprApi(
    baseUrl: String = System.getenv("HOUSEKPR_BASE_URL") ?: "http://localhost:8080"
) {
    private val base = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client: HttpClient = HttpClient.newBuilder()
//        .connectTimeout(Duration.ofSeconds(2))
        .build()

    fun login(email: String, password: String): String {
        val url = "$base/api/login"
        val body = json.encodeToString(LoginRequest.serializer(), LoginRequest(email, password))
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(2))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw RuntimeException("Auth failed: HTTP ${'$'}{resp.statusCode()} - ${'$'}{resp.body()}")
        }
        val parsed = json.decodeFromString(LoginResponse.serializer(), resp.body())
        return parsed.accessToken
    }

    fun getCarOverview(carNumber: String, token: String): CarOverviewResponse {
        val normalized = carNumber.trim()
        val url = "$base/api/access/overview/${'$'}normalized?active=true"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(2))
            .header("Authorization", "Bearer ${'$'}token")
            .GET()
            .build()
        val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() == 404) {
            return CarOverviewResponse() // empty
        }
        if (resp.statusCode() !in 200..299) {
            throw RuntimeException("Overview failed: HTTP ${'$'}{resp.statusCode()} - ${'$'}{resp.body()}")
        }
        return json.decodeFromString(CarOverviewResponse.serializer(), resp.body())
    }
}

fun formatCarOverview(r: CarOverviewResponse): String {
    if (r.carNumber.isNullOrBlank() && r.carDescription.isNullOrBlank()) {
        return "Ничего не найдено по указанному номеру."
    }
    val b = StringBuilder()
    r.carNumber?.let { b.appendLine("Номер: ${'$'}it") }
    r.carDescription?.let { b.appendLine("Авто: ${'$'}it") }
    if (!r.ownerName.isNullOrBlank()) {
        b.appendLine("Владелец: ${'$'}{r.ownerName}")
    }
    if (!r.ownerRooms.isNullOrBlank()) {
        b.appendLine("Помещения: ${'$'}{r.ownerRooms}")
    }
    if (!r.phoneLabel.isNullOrBlank() || !r.phoneNumber.isNullOrBlank()) {
        val phone = r.phoneNumber ?: "—"
        val label = r.phoneLabel ?: "—"
        b.appendLine("Телефон: ${'$'}phone (${'$'}label)")
    }
    if (r.overviewAreas.isNotEmpty()) {
        b.appendLine()
        b.appendLine("Зоны доступа:")
        r.overviewAreas.forEach { area ->
            val places = if (area.places.isNotEmpty()) area.places.joinToString(", ") else "—"
            b.appendLine("• ${'$'}{area.areaName}: ${'$'}places")
        }
    }
    return b.toString().trimEnd()
}
