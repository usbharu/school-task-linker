package dev.usbharu.stl.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        // NotFoundExceptionがスローされた場合
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                FreeMarkerContent("404.ftl", mapOf("message" to (cause.message ?: "ページが見つかりませんでした。")))
            )
        }

        // その他の認証関連のエラー (例: ログインしていないのに保護されたページにアクセス)
        exception<AuthenticationException> { call, cause ->
            call.respondRedirect("/login")
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }

        // ステータスコードに応じたエラーページ
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                FreeMarkerContent("404.ftl", mapOf("message" to "お探しのページは見つかりませんでした。URLをご確認ください。"))
            )
        }

        // ハンドルされていない例外 (500 Internal Server Error)
        exception<Throwable> { call, cause ->
            // 本番環境では詳細なエラーをユーザーに見せない
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                FreeMarkerContent("500.ftl", mapOf("message" to "サーバー内部でエラーが発生しました。"))
            )
        }
    }
}

// 認証・認可プラグインがスローする例外クラス
class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
