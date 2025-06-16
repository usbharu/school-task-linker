package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.Users
import dev.usbharu.stl.model.Users.oauthState
import dev.usbharu.stl.oauth.GoogleOAuth
import dev.usbharu.stl.plugins.UserSession
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.util.*

// Googleからのトークンレスポンスを格納するデータクラス
@Serializable
data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String,
    @SerialName("token_type") val tokenType: String
)

fun Route.oauthRoutes() {
    // Google認証を開始するルート
    get("/oauth/google/start") {
        val userSession = call.principal<UserSession>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val state = UUID.randomUUID().toString()
        // CSRF攻撃を防ぐための`state`をセッションに保存
        dbQuery {
            Users.update({ Users.id eq userSession.userId }) {
                it[oauthState] = state
            }
        }

        val url = URLBuilder(GoogleOAuth.AUTHORIZATION_URL).apply {
            parameters.append("client_id", GoogleOAuth.CLIENT_ID)
            parameters.append("redirect_uri", GoogleOAuth.REDIRECT_URI)
            parameters.append("response_type", "code")
            parameters.append("scope", GoogleOAuth.SCOPES)
            parameters.append("state", state)
            parameters.append("access_type", "offline") // リフレッシュトークンを取得するため
            parameters.append("prompt", "consent")      // 毎回同意画面を表示
        }.build()

        call.respondRedirect(url.toString())
    }

    // Googleからのコールバックを処理するルート
    get("/oauth/google/callback") {
        val code = call.request.queryParameters["code"]
        val receivedState = call.request.queryParameters["state"]
        val userSession = call.principal<UserSession>() ?: return@get call.respond(HttpStatusCode.Unauthorized)

        // DBからstateトークンを取得して検証
        val storedState = dbQuery {
            Users.select { Users.id eq userSession.userId }.singleOrNull()?.get(oauthState)
        }

        if (storedState == null || receivedState != storedState) {
            return@get call.respond(HttpStatusCode.BadRequest, "Invalid state")
        }

        // 検証後、DBのstateトークンをクリア
        dbQuery {
            Users.update({ Users.id eq userSession.userId }) {
                it[oauthState] = null
            }
        }

        if (code == null) {
            return@get call.respond(HttpStatusCode.BadRequest, "No code received")
        }

        // 認可コードをアクセストークンに交換
        val response: HttpResponse = GoogleOAuth.httpClient.post(GoogleOAuth.TOKEN_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            parameter("client_id", GoogleOAuth.CLIENT_ID)
            parameter("client_secret", GoogleOAuth.CLIENT_SECRET)
            parameter("code", code)
            parameter("grant_type", "authorization_code")
            parameter("redirect_uri", GoogleOAuth.REDIRECT_URI)
        }

        if (response.status == HttpStatusCode.OK) {
            val tokenResponse = response.body<GoogleTokenResponse>()

            // トークンをデータベースに保存 (upsertを使用)
            dbQuery {
                TodoServices.upsert(TodoServices.userId) {
                    it[userId] = userSession.userId
                    it[serviceName] = "GoogleTasks"
                    it[accessToken] = tokenResponse.accessToken
                    if (tokenResponse.refreshToken != null) {
                        it[refreshToken] = tokenResponse.refreshToken
                    }
                    it[expiresAt] = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
                }
            }
            call.respondRedirect("/settings?status=google_connected")
        } else {
            val errorBody = response.bodyAsText()
            application.log.error("Failed to get token: ${response.status} - $errorBody")
            call.respondRedirect("/settings?error=google_failed")
        }
    }
}
