package dev.usbharu.stl.oauth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json

object GoogleOAuth {
    lateinit var CLIENT_ID: String
        private set // 外部からの変更を禁止
    lateinit var CLIENT_SECRET: String
        private set // 外部からの変更を禁止

    /**
     * 設定ファイルまたは環境変数からOAuth認証情報を読み込んで初期化します。
     */
    fun init(config: ApplicationConfig) {
        CLIENT_ID = config.propertyOrNull("app.oauth.google.client-id")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GOOGLE_CLIENT_ID")
                    ?: throw IllegalArgumentException("Google Client ID is not set in application.conf or environment variables.")

        CLIENT_SECRET = config.propertyOrNull("app.oauth.google.client-secret")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GOOGLE_CLIENT_SECRET")
                    ?: throw IllegalArgumentException("Google Client Secret is not set in application.conf or environment variables.")
    }

    const val AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    const val REDIRECT_URI = "http://localhost:8080/oauth/google/callback"

    val SCOPES = listOf(
        "https://www.googleapis.com/auth/tasks",
        "https://www.googleapis.com/auth/userinfo.profile"
    ).joinToString(" ")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }
}
