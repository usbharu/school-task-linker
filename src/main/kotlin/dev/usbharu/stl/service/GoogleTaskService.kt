package dev.usbharu.stl.service

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.GoogleTask
import dev.usbharu.stl.model.GoogleTaskList
import dev.usbharu.stl.model.GoogleTaskListResponse
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.oauth.GoogleOAuth
import dev.usbharu.stl.routes.GoogleTokenResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Google Tasks APIから返されるタスク一覧のレスポンス
@Serializable
private data class GoogleTasksResponse(val items: List<GoogleApiTask>? = null)

// Google Tasks APIから返される個々のタスク情報
@Serializable
private data class GoogleApiTask(val id: String, val title: String, val notes: String? = null)

// アプリケーション内部で扱うタスク情報をまとめたデータクラス
data class AppTask(
    val internalId: Int,
    val title: String,
    val notes: String,
    val due: ZonedDateTime?
)

class GoogleTasksService(private val userId: Int) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val internalIdRegex = Regex("""\[Internal-ID:\s*(\d+)]""")

    /**
     * 複数のタスクをGoogle ToDoリストと一括で同期する
     */
    suspend fun syncTasksInBatch(tasksToSync: List<AppTask>) {
        if (tasksToSync.isEmpty()) {
            log.info("No tasks to sync for user $userId.")
            return
        }

        val serviceInfo = dbQuery {
            TodoServices.selectAll()
                .where { (TodoServices.userId eq userId) and (TodoServices.serviceName eq "GoogleTasks") }
                .singleOrNull()
        } ?: return

        val token = getValidAccessToken(serviceInfo) ?: run {
            log.error("No valid token for user $userId. Skipping sync.")
            return
        }

        val taskListId = serviceInfo[TodoServices.selectedTaskListId] ?: "@default"

        // 1. Google Tasksから既存のタスクをすべて取得し、重複チェック用のマップを作成
        log.info("Fetching existing tasks from Google Tasks for batch check...")
        val existingTaskMap = getAllTasksAsMap(taskListId, token)
        log.info("Found ${existingTaskMap.size} existing tasks with internal IDs.")

        // 2. 重複していない、本当に新しいタスクだけをリストアップ
        val newTasksToCreate = tasksToSync.filter { !existingTaskMap.containsKey(it.internalId) }

        if (newTasksToCreate.isEmpty()) {
            log.info("All tasks are already synced. Nothing to do.")
            return
        }

        log.info("Found ${newTasksToCreate.size} new tasks to create in Google Tasks.")

        // 3. 新しいタスクを一つずつ作成
        for (task in newTasksToCreate) {
            createTask(task, taskListId, token)
        }
    }

    /**
     * Google Tasksから全タスクを取得し、[内部ID -> GoogleタスクID] のマップを生成する
     */
    private suspend fun getAllTasksAsMap(taskListId: String, token: TodoServiceToken): Map<Int, String> {
        val existingMap = mutableMapOf<Int, String>()
        try {
            val response = GoogleOAuth.httpClient.get("https://tasks.googleapis.com/tasks/v1/lists/$taskListId/tasks") {
                bearerAuth(token.accessToken)
                parameter("showCompleted", "true")
            }
            if (!response.status.isSuccess()) return emptyMap()

            val tasks = response.body<GoogleTasksResponse>().items ?: return emptyMap()

            for (task in tasks) {
                val match = task.notes?.let { internalIdRegex.find(it) }
                if (match != null) {
                    val internalId = match.groupValues[1].toIntOrNull()
                    if (internalId != null) {
                        existingMap[internalId] = task.id
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Exception while fetching all tasks for duplicate check.", e)
        }
        return existingMap
    }

    /**
     * Google ToDoリストに単一のタスクを作成する
     */
    private suspend fun createTask(task: AppTask, taskListId: String, token: TodoServiceToken) {
        val notesWithId = "${task.notes}\n\n[Internal-ID: ${task.internalId}]"
        val googleTask = GoogleTask(
            title = task.title,
            notes = notesWithId,
            due = task.due?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
        try {
            val response = GoogleOAuth.httpClient.post("https://tasks.googleapis.com/tasks/v1/lists/$taskListId/tasks") {
                bearerAuth(token.accessToken)
                contentType(ContentType.Application.Json)
                setBody(googleTask)
            }
            if (response.status.isSuccess()) {
                log.info("Successfully created task '${task.title}' (Internal-ID: ${task.internalId}) in Google Tasks.")
            } else {
                log.error("Failed to create task in Google Tasks. Status: ${response.status}, Body: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            log.error("Exception while creating task in Google Tasks.", e)
        }
    }

    // getTaskListsとgetValidAccessTokenは変更なし
    suspend fun getTaskLists(): List<GoogleTaskList> {
        val serviceInfo = dbQuery {
            TodoServices.selectAll()
                .where { (TodoServices.userId eq userId) and (TodoServices.serviceName eq "GoogleTasks") }
                .singleOrNull()
        } ?: return emptyList()

        val token = getValidAccessToken(serviceInfo) ?: return emptyList()

        return try {
            val response = GoogleOAuth.httpClient.get("https://tasks.googleapis.com/tasks/v1/users/@me/lists") {
                bearerAuth(token.accessToken)
            }
            if (response.status.isSuccess()) {
                response.body<GoogleTaskListResponse>().items
            } else {
                log.error("Failed to fetch task lists for user $userId. Status: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            log.error("Exception while fetching task lists for user $userId", e)
            emptyList()
        }
    }

    private suspend fun getValidAccessToken(serviceInfo: org.jetbrains.exposed.sql.ResultRow): TodoServiceToken? {
        var accessToken = serviceInfo[TodoServices.accessToken]
        val refreshToken = serviceInfo[TodoServices.refreshToken]
        val expiresAt = serviceInfo[TodoServices.expiresAt]

        if (System.currentTimeMillis() >= expiresAt) {
            log.info("Access token expired for user $userId. Refreshing token...")
            if (refreshToken == null) {
                log.error("No refresh token available for user $userId.")
                return null
            }
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
