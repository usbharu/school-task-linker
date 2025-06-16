package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.MailSettings
import dev.usbharu.stl.model.RegexRules
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.toRegexRule
import dev.usbharu.stl.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.upsert

fun Route.settingsRoutes() {
    // 設定ページの表示
    get("/settings") {
        val userSession = call.principal<UserSession>()!!

        // Google Tasksとの連携状態を確認
        val googleService = dbQuery {
            TodoServices.select {
                (TodoServices.userId eq userSession.userId) and (TodoServices.serviceName eq "GoogleTasks")
            }.singleOrNull()
        }
        val isGoogleConnected = googleService != null
        val regexRules = dbQuery {
            RegexRules.select { RegexRules.userId eq userSession.userId }.map(::toRegexRule)
        }

        // テンプレートに渡すデータ
        val data = mapOf(
            "user" to userSession,
            "mailSettings" to mapOf<String, Any>(), // DBから取得した設定に置き換える
            "isGoogleConnected" to isGoogleConnected,
            "regexRules" to regexRules, // ルール一覧をテンプレートに渡す
            "status" to call.request.queryParameters["status"],
            "error" to call.request.queryParameters["error"]
        )

        call.respond(FreeMarkerContent("settings.ftl", data))
    }

    // 各種設定の保存処理 (例: メール設定)
    post("/settings/mail") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val host = params["host"] ?: return@post call.respondRedirect("/settings?error=mail_missing_fields#mail-settings")
        val port = params["port"]?.toIntOrNull() ?: return@post call.respondRedirect("/settings?error=mail_missing_fields#mail-settings")
        val email = params["email"] ?: return@post call.respondRedirect("/settings?error=mail_missing_fields#mail-settings")
        val password = params["password"] ?: return@post call.respondRedirect("/settings?error=mail_missing_fields#mail-settings")

        // 既存の設定を更新、なければ新規作成
        dbQuery {
            MailSettings.upsert(MailSettings.userId) {
                it[userId] = userSession.userId
                it[MailSettings.host] = host
                it[MailSettings.port] = port
                it[MailSettings.email] = email
                it[MailSettings.password] = password // 注意: 本番環境では暗号化を強く推奨
            }
        }
        call.respondRedirect("/settings?status=mail_saved#mail-settings")
    }
}
