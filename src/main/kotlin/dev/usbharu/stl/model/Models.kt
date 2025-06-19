package dev.usbharu.stl.model

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

// メールサーバー設定を格納するテーブル
object MailSettings : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val host = varchar("host", 255)
    val port = integer("port")
    val email = varchar("email", 255)
    val password = varchar("password", 255) // 暗号化して保存することを強く推奨
    override val primaryKey = PrimaryKey(id)
}

data class RegexRule(
    val id: Int,
    val name: String,
    val pattern: String,
    val category: String
)

// SQLの実行結果(ResultRow)をデータクラスに変換するヘルパー関数
fun toRegexRule(row: ResultRow): RegexRule =
    RegexRule(
        id = row[RegexRules.id],
        name = row[RegexRules.name],
        pattern = row[RegexRules.pattern],
        category = row[RegexRules.category]
    )

// 正規表現ルールを格納するテーブル
object RegexRules : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 255)
    val pattern = varchar("pattern", 1024)
    val category = varchar("category", 128) // e.g., "NEW_ASSIGNMENT", "DEADLINE_NOTICE", "EVENT"
    override val primaryKey = PrimaryKey(id)
}


// 連携するToDoサービスの情報（特にOAuthトークン）を格納するテーブル
object TodoServices : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val serviceName = varchar("service_name", 128)
    val accessToken = varchar("access_token", 2048)
    val refreshToken = varchar("refresh_token", 2048).nullable()
    val expiresAt = long("expires_at")
    // ユーザーが選択したGoogle ToDoリストのIDを保存するカラム
    val selectedTaskListId = varchar("selected_task_list_id", 255).nullable()
    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, serviceName)
    }
}