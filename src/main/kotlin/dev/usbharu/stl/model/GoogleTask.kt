package dev.usbharu.stl.model


import kotlinx.serialization.Serializable

/**
 * Google Tasks APIにタスク作成リクエストを送信するためのデータクラス
 */
@Serializable
data class GoogleTask(
        val title: String,
        val notes: String? = null,
        val due: String? = null // RFC3339 format (e.g., "2025-06-12T23:59:59.000Z")
)

/**
 * Google Tasks APIから返されるタスクリストの単一アイテム
 */
@Serializable
data class GoogleTaskList(
        val id: String,
        val title: String
)

/**
 * Google Tasks APIから返されるタスクリスト一覧のレスポンス全体
 */
@Serializable
data class GoogleTaskListResponse(
        val items: List<GoogleTaskList> = emptyList()
)
