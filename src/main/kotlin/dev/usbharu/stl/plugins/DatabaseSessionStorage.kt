package dev.usbharu.stl.plugins

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Sessions
import dev.usbharu.stl.model.Sessions.data
import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

/**
 * Exposed(データベース)を利用してセッションを永続化するためのカスタムSessionStorage
 */
class DatabaseSessionStorage : SessionStorage {

    // セッションを書き込む（新規作成 or 更新）
    override suspend fun write(id: String, value: String) {
        dbQuery {
            val currentTime = System.currentTimeMillis()
            val existingSession = Sessions.selectAll().where { Sessions.id eq id }.singleOrNull()

            if (existingSession != null) {
                // あれば更新
                Sessions.update({ Sessions.id eq id }) {
                    it[data] = value
                    it[updatedAt] = currentTime // 最終更新日時を更新
                }
            } else {
                // なければ新規作成
                Sessions.insert {
                    it[Sessions.id] = id
                    it[data] = value
                    it[updatedAt] = currentTime // 最終更新日時を設定
                }
            }
        }
    }

    // セッションを読み込む
    override suspend fun read(id: String): String {
        return dbQuery {
            Sessions.selectAll().where { Sessions.id eq id }.singleOrNull()?.get(data)
        } ?: throw NoSuchElementException("Session $id not found")
    }

    // セッションを無効化する（削除）
    override suspend fun invalidate(id: String) {
        dbQuery {
            Sessions.deleteWhere { Sessions.id eq id }
        }
    }
}
