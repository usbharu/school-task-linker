package dev.usbharu.stl.plugins

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Users
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.select
import org.mindrot.jbcrypt.BCrypt

// セッションに保存するユーザー情報を定義
@Serializable
data class UserSession(val userId: Int, val username: String) : Principal

fun Application.configureSecurity() {
    install(Sessions) {
        // "user_session" という名前でCookieにセッション情報を保存
        cookie<UserSession>("user_session", DatabaseSessionStorage()) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 7 // 1週間有効
        }
    }

    install(Authentication) {
        // フォーム認証の設定 ("auth-form"という名前)
        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"

            // ユーザー名とパスワードを検証するロジック
            validate { credentials ->
                val user = dbQuery {
                    Users.select { Users.username eq credentials.name }.singleOrNull()
                }
                println(user)
                println(credentials.password)
                // ユーザーが存在し、かつパスワードが一致すればUserSessionを返す
                if (user != null && BCrypt.checkpw(credentials.password, user[Users.passwordHash])) {
                    UserIdPrincipal( user[Users.username])
                } else {
                    null // 失敗した場合はnullを返す
                }
            }

            // 認証に失敗した場合、エラー付きでログインページにリダイレクト
            challenge {
                call.respondRedirect("/login?error=invalid_credentials")
            }
        }

        // セッション認証の設定 ("auth-session"という名前)
        // Cookieに保存されたUserSessionを検証する
        session<UserSession>("auth-session") {

            validate { session ->
                // ここでセッションが有効か（例：ユーザーがまだ存在するか）を
                // DBに問い合わせることも可能。今回はシンプルにセッションがあればOKとする。
                session
            }
            // セッションが無効（ログインしていない）場合、ログインページにリダイレクト
            challenge {
                call.respondRedirect("/login")
            }
        }
    }
}
