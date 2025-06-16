package dev.usbharu.stl.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * ダッシュボード一覧表示用のデータクラス
 */
data class Task(
    val id: Int,
    val courseName: String,
    val taskName: String,
    val deadline: LocalDateTime?,
    val deadlineJstFormatted: String,
    val overdue: Boolean
)

/**
 * モーダルでの詳細表示用データクラス (本文を含む)
 */
@Serializable
data class TaskDetail(
    val id: Int,
    val courseName: String,
    val taskName: String,
    val deadlineJstFormatted: String,
    val body: String
)

/**
 * SQLの実行結果(ResultRow)をTaskデータクラスに変換するヘルパー関数
 */
fun toTask(row: ResultRow): Task {
    val deadlineUtc = row[Tasks.deadline]
    var isOverdue = false

    val deadlineJstFormatted = deadlineUtc?.let {
        val jstZone = ZoneId.of("Asia/Tokyo")
        val deadlineJst = it.atZone(ZoneId.of("UTC")).withZoneSameInstant(jstZone)

        val nowJst = ZonedDateTime.now(jstZone)
        isOverdue = deadlineJst.isBefore(nowJst)

        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        deadlineJst.format(formatter)
    } ?: "期限なし"

    return Task(
        id = row[Tasks.id],
        courseName = row[Tasks.courseName],
        taskName = row[Tasks.taskName],
        deadline = deadlineUtc,
        deadlineJstFormatted = deadlineJstFormatted,
        overdue = isOverdue
    )
}

/**
 * SQLの実行結果(ResultRow)をTaskDetailデータクラスに変換するヘルパー関数
 */
fun toTaskDetail(row: ResultRow): TaskDetail {
    val deadlineUtc = row[Tasks.deadline]
    val deadlineJstFormatted = deadlineUtc?.let {
        val jstZone = ZoneId.of("Asia/Tokyo")
        val deadlineJst = it.atZone(ZoneId.of("UTC")).withZoneSameInstant(jstZone)
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        deadlineJst.format(formatter)
    } ?: "期限なし"

    return TaskDetail(
        id = row[Tasks.id],
        courseName = row[Tasks.courseName],
        taskName = row[Tasks.taskName],
        deadlineJstFormatted = deadlineJstFormatted,
        body = row[Tasks.body]
    )
}


/**
 * 抽出した課題を格納するテーブル定義
 */
object Tasks : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val emailUid = varchar("email_uid", 255)

    val taskName = varchar("task_name", 512)
    val courseName = varchar("course_name", 512)
    val deadline = datetime("deadline").nullable()
    val body = text("body")

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(userId, emailUid)
    }
}
