package dev.usbharu.stl

import dev.usbharu.stl.db.DatabaseFactory
import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Sessions
import dev.usbharu.stl.model.Users
import dev.usbharu.stl.model.toUser
import dev.usbharu.stl.oauth.GoogleOAuth
import dev.usbharu.stl.plugins.configureRouting
import dev.usbharu.stl.plugins.configureSecurity
import dev.usbharu.stl.plugins.configureSerialization
import dev.usbharu.stl.plugins.configureStatusPages
import dev.usbharu.stl.plugins.configureTemplating
import dev.usbharu.stl.service.MailReaderService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

/**
 * アプリケーションのエントリーポイント。
 * application.confを自動的に読み込むためにEngineMainを使用します。
 */
fun main(args: Array<String>): Unit = EngineMain.main(args)

/**
 * Ktorアプリケーションのメインモジュール。
 * application.confから "ktor.application.modules" で指定されると呼び出されます。
 */
@Suppress("unused")
fun Application.module() {
    // application.confから設定を読み込む
    val config = environment.config

    // 設定ファイルを使って各モジュールを初期化
    DatabaseFactory.init(config)
    GoogleOAuth.init(config)

    // Ktorプラグインを設定
    configureTemplating()
    configureSerialization()
    configureSecurity()
    configureRouting()
    configureStatusPages()

    // --- ここから自動実行タスク ---

    // 設定ファイルから間隔を取得
    val sessionCleanupInterval = config.property("app.scheduler.session-cleanup-interval").getString().toLong()
    val emailCheckInterval = config.property("app.scheduler.email-check-interval").getString().toLong()
    val initialDelay = config.property("app.scheduler.initial-delay").getString().toLong()

    // 古いセッションを定期的に削除するバックグラウンドタスク
    launch {
        val log = environment.log
        delay(initialDelay)
        while (true) {
            val sessionMaxAge = 7 * 24 * 60 * 60 * 1000L
            val expiryTime = System.currentTimeMillis() - sessionMaxAge
            log.info("Running session cleanup task...")
            try {
                val deletedCount = dbQuery { Sessions.deleteWhere { Sessions.updatedAt less expiryTime } }
                log.info("Cleaned up $deletedCount expired sessions.")
            } catch (e: Exception) {
                log.error("Error during session cleanup task", e)
            }
            delay(sessionCleanupInterval)
        }
    }

    // 全ユーザーのメールを定期的にチェックするバックグラウンドタスク
    launch {
        val log = environment.log
        delay(initialDelay)
        while (true) {
            log.info("Starting periodic email check for all users...")
            try {
                val users = dbQuery { Users.selectAll().map(::toUser) }
                log.info("Found ${users.size} users to check.")

                for (user in users) {
                    try {
                        log.info("Checking emails for user: ${user.username} (ID: ${user.id})")
                        MailReaderService(user.id).checkAndProcessEmails()
                    } catch (e: Exception) {
                        log.error("Failed to check emails for user ${user.username}", e)
                    }
                }
                log.info("Finished periodic email check for all users.")
            } catch (e: Exception) {
                log.error("An error occurred during the periodic email check scheduler.", e)
            }
            delay(emailCheckInterval)
        }
    }
}
