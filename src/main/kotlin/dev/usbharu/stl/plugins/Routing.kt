package dev.usbharu.stl.plugins

import dev.usbharu.stl.db.DatabaseFactory
import dev.usbharu.stl.model.Users
import dev.usbharu.stl.routes.*
import dev.usbharu.stl.service.MailReaderService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.selectAll

fun Application.configureRouting() {
    routing {
        // 静的ファイル(CSS, JS)の配信
        staticResources("/static", "static")

        // ログイン/ログアウト/登録などの認証関連ルート
        authRoutes()

        // 認証が必要なルートグループ
        authenticate("auth-form") {
            // ログイン後のリダイレクト先
            post("/login") {
                val principal = call.principal<UserIdPrincipal>()!!
                //dbからuserを取得してUserSessionに詰める
                val user = DatabaseFactory.dbQuery {
                    Users.selectAll().where { Users.username eq principal.name }.singleOrNull()
                }
                if (user != null) {
                    call.sessions.set(UserSession(user[Users.id], user[Users.username]))
                }

//                call.sessions.set(UserSession())
//                call.sessions.set(principal)
                call.respondRedirect("/dashboard")
            }
        }

        // セッション認証が必要なルートグループ
        authenticate("auth-session") {
            // ログイン後のメインページ
            dashboardRoutes()
            oauthRoutes()
            // 各種設定ページ
            settingsRoutes()
            regexRoutes()
            taskRoutes()
            googleRoutes()


            // OAuth連携用のルート (ここに実装)
            // 例: /oauth/googletasks/start, /oauth/googletasks/callback

            // 手動メールチェックのトリガー (ここに実装)
            post("/check-mail") {
                val userSession = call.principal<UserSession>()!!
                // メールチェックは時間がかかる可能性があるので、別Coroutineで実行
                launch {
                    MailReaderService(userSession.userId).checkAndProcessEmails()
                }
                call.respondRedirect("/dashboard?status=manual_check_started")
            }
        }

        // ルートURLへのアクセスはログインページへ
        get("/") {
            call.respondRedirect("/login")
        }
    }
}
