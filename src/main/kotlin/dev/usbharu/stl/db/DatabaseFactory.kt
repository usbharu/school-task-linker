package dev.usbharu.stl.db

import dev.usbharu.stl.model.MailSettings
import dev.usbharu.stl.model.RegexRules
import dev.usbharu.stl.model.Sessions
import dev.usbharu.stl.model.Tasks
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.Users
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    /**
     * 設定ファイルから読み込んだ情報を使ってデータベースを初期化します。
     */
    fun init(config: ApplicationConfig) {
        // 設定ファイルからデータベース接続情報を取得
        val driverClassName = config.property("app.database.driver").getString()
        val jdbcURL = config.property("app.database.jdbc-url").getString()
        val database = Database.connect(jdbcURL, driverClassName)

        transaction(database) {
            SchemaUtils.create(Users, MailSettings, RegexRules, TodoServices, Sessions, Tasks)
            SchemaUtils.createMissingTablesAndColumns(Users, MailSettings, RegexRules, TodoServices, Sessions, Tasks)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
