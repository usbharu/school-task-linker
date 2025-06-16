package dev.usbharu.stl.service

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.GoogleTask
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.oauth.GoogleOAuth
import dev.usbharu.stl.routes.GoogleTokenResponse
import io.ktor.client.statement.bodyAsText

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class GoogleTasksService(private val userId: Int) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Google ToDoリストに新しいタスクを作成する
     */
    suspend fun createTask(taskTitle: String, notes: String, due: ZonedDateTime?) {
        val tokenInfo = getValidAccessToken() ?: run {
            log.error("No valid token found for user $userId. Skipping Google Task creation.")
            return
        }

        val task = GoogleTask(
            title = taskTitle,
            notes = notes,
            // Google Tasks APIはRFC3339形式の日時を要求する
            due = due?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )

        try {
            // デフォルトのタスクリスト '@default' にタスクを追加
            val response = GoogleOAuth.httpClient.post("https://tasks.googleapis.com/tasks/v1/lists/@default/tasks") {
                bearerAuth(tokenInfo.accessToken)
                contentType(ContentType.Application.Json)
                setBody(task)
            }
            if (response.status.isSuccess()) {
                log.info("Successfully created task '$taskTitle' in Google Tasks for user $userId.")
            } else {
                log.error("Failed to create task in Google Tasks for user $userId. Status: ${response.status}, Body: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            log.error("Exception while creating task in Google Tasks for user $userId.", e)
        }
    }

    /**
     * 有効なアクセストークンを取得する。期限切れの場合はリフレッシュを試みる。
     */
    private suspend fun getValidAccessToken(): TodoServiceToken? {
        val serviceInfo = dbQuery {
            TodoServices.select { (TodoServices.userId eq userId) and (TodoServices.serviceName eq "GoogleTasks") }
                .singleOrNull()
        } ?: return null

        var accessToken = serviceInfo[TodoServices.accessToken]
        val refreshToken = serviceInfo[TodoServices.refreshToken]
        val expiresAt = serviceInfo[TodoServices.expiresAt]

        // アクセストークンの有効期限をチェック
        if (System.currentTimeMillis() >= expiresAt) {
            log.info("Access token expired for user $userId. Refreshing token...")
            if (refreshToken == null) {
                log.error("No refresh token available for user $userId to refresh access token.")
                return null
            }

            // リフレッシュトークンを使って新しいアクセストークンを取得
            val response = GoogleOAuth.httpClient.post(GoogleOAuth.TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                parameter("client_id", GoogleOAuth.CLIENT_ID)
                parameter("client_secret", GoogleOAuth.CLIENT_SECRET)
                parameter("refresh_token", refreshToken)
                parameter("grant_type", "refresh_token")
            }

            if (response.status.isSuccess()) {
                val tokenResponse = response.body<GoogleTokenResponse>()
                val newAccessToken = tokenResponse.accessToken
                val newExpiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)

                // データベースのトークン情報を更新
                dbQuery {
                    TodoServices.update({ (TodoServices.userId eq userId) and (TodoServices.serviceName eq "GoogleTasks") }) {
                        it[TodoServices.accessToken] = newAccessToken
                        it[TodoServices.expiresAt] = newExpiresAt
                    }
                }
                log.info("Successfully refreshed access token for user $userId.")
                accessToken = newAccessToken
            } else {
                log.error("Failed to refresh token for user $userId. Status: ${response.status}, Body: ${response.bodyAsText()}")
                return null
            }
        }

        return TodoServiceToken(accessToken)
    }

    private data class TodoServiceToken(val accessToken: String)
}
