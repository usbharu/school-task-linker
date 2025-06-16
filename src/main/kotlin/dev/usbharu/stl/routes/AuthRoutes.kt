package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Users
import dev.usbharu.stl.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.insert
import org.mindrot.jbcrypt.BCrypt

fun Route.authRoutes() {

    // ログインページの表示
    get("/login") {
        // もしエラーがあればテンプレートに渡す
        val error = call.request.queryParameters["error"]
        call.respond(FreeMarkerContent("login.ftl", mapOf("error" to error)))
    }

    // ログアウト処理
    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/login")
    }

    // 新規登録ページの表示
    get("/register") {
        call.respond(FreeMarkerContent("register.ftl", null))
    }

    // 新規登録処理
    post("/register") {
        val params = call.receiveParameters()
        val username = params["username"] ?: return@post call.respondRedirect("/register?error=missing_fields")
        val password = params["password"] ?: return@post call.respondRedirect("/register?error=missing_fields")

        // パスワードをハッシュ化
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        try {
            // ユーザーをデータベースに登録
            val newUserRow = dbQuery {
                Users.insert {
                    it[Users.username] = username
                    it[Users.passwordHash] = passwordHash
                }
            }

            // 登録したユーザーのIDを取得
            val newUserId = newUserRow[Users.id]

            // そのままセッションを作成して自動ログイン
            call.sessions.set(UserSession(userId = newUserId, username = username))

            // ダッシュボードにリダイレクト
            call.respondRedirect("/dashboard")

        } catch (e: Exception) {
            // ユーザー名が重複している場合など
            call.respondRedirect("/register?error=user_exists")
        }
    }
}
