package dev.usbharu.stl.model

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

// ユーザー情報を格納するテーブル
object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    // OAuth認証フロー中にstateトークンを一時的に保存するカラム
    val oauthState = varchar("oauth_state", 128).nullable()
    override val primaryKey = PrimaryKey(id)
}

data class User(val id: Int, val username: String)

/**
 * SQLの実行結果(ResultRow)をUserデータクラスに変換するヘルパー関数
 */
fun toUser(row: ResultRow): User =
    User(
        id = row[Users.id],
        username = row[Users.username]
    )