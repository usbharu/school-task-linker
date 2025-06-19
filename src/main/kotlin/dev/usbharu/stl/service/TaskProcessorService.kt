package dev.usbharu.stl.service

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.RegexRule
import dev.usbharu.stl.model.Tasks
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

class TaskProcessorService(
    private val userId: Int,
    private val rules: List<RegexRule>,
    private val isGoogleConnected: Boolean
) {
    suspend fun process(emails: List<MailReaderService.SimpleEmail>) {
        if (emails.isEmpty()) {
            println("No new emails to process for user $userId.")
        } else {
            println("Processing ${emails.size} new emails for user $userId...")
            // 1. 新着メールを解析し、DBを更新する
            for (email in emails) {
                for (rule in rules) {
                    val regex = Regex(rule.pattern)
                    if (regex.matches(email.subject)) {
                        val match = regex.find(email.subject)!!
                        val taskNameFromSubject = if (match.groupValues.size > 1) match.groupValues[1].trim() else match.value.trim()

                        when (rule.category) {
                            "DEADLINE_NOTICE" -> parseAndSaveDeadlineNotice(taskNameFromSubject, email)
                            // TODO: 他のカテゴリの解析メソッドもここに追加
                        }
                        break
                    }
                }
            }
        }

        // 2. Google連携が有効な場合、DBの全タスクを対象に同期処理を実行
        if (!isGoogleConnected) {
            println("Google sync is disabled for user $userId. Skipping sync.")
            return
        }

        println("Fetching all tasks from DB for batch sync...")
        val allTasksFromDb = dbQuery {
            Tasks.selectAll().where { Tasks.userId eq this@TaskProcessorService.userId }.map(::toAppTaskFromDb)
        }

        if (allTasksFromDb.isNotEmpty()) {
            println("Starting batch sync with Google ToDo for ${allTasksFromDb.size} total tasks...")
            GoogleTasksService(this.userId).syncTasksInBatch(allTasksFromDb)
        } else {
            println("No tasks found in DB to sync with Google ToDo.")
        }
    }

    /**
     * `DEADLINE_NOTICE`カテゴリのメールを解析し、DBに保存する
     */
    private suspend fun parseAndSaveDeadlineNotice(taskNameFromSubject: String, email: MailReaderService.SimpleEmail) {
        println("--- Parsing email (UID: ${email.uid}) for category: DEADLINE_NOTICE ---")
        val bodyLines = email.body.lines()

        var courseName = "（講義名不明）"
        val noticeLineIndex = bodyLines.indexOfFirst { it.contains("課題の期限が近づいています") }
        if (noticeLineIndex > 0) { courseName = bodyLines[noticeLineIndex - 1] }

        var finalTaskName = taskNameFromSubject
        if (noticeLineIndex != -1 && noticeLineIndex + 1 < bodyLines.size) { finalTaskName = bodyLines[noticeLineIndex + 1] }

        val deadlinePattern = Regex("""期限\s*[:：]\s*(\d{4}年\d{1,2}月\d{1,2}日\s*\d{1,2}:\d{2}:\d{2})""")
        val deadlineMatch = deadlinePattern.find(email.body)
        var deadlineUtc: LocalDateTime? = null
        if (deadlineMatch != null) {
            val deadlineString = deadlineMatch.groupValues[1].trim()
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 H:mm:ss", Locale.JAPANESE)
                val localDateTime = LocalDateTime.parse(deadlineString, formatter)
                val zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Tokyo"))
                deadlineUtc = zonedDateTime.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                println("Failed to parse deadline: '$deadlineString'.")
            }
        }

        // DBに保存または更新
        saveOrUpdateTaskInDb(email.uid, finalTaskName, courseName, deadlineUtc, email.body)
    }

    /**
     * DBのResultRowをGoogle同期用のAppTaskデータクラスに変換する
     */
    private fun toAppTaskFromDb(row: ResultRow): AppTask {
        val courseName = row[Tasks.courseName]
        val taskName = row[Tasks.taskName]
        val deadlineUtc = row[Tasks.deadline]

        val deadlineZoned = deadlineUtc?.atZone(ZoneId.of("UTC"))?.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))

        val notesBody = """
            講義名: $courseName
            
            --- 元のメール本文 ---
            ${row[Tasks.body]}
        """.trimIndent()

        return AppTask(
            internalId = row[Tasks.id],
            title = "[$courseName] $taskName",
            notes = notesBody,
            due = deadlineZoned
        )
    }

    /**
     * 抽出した課題情報をデータベースに保存または更新する
     */
    private suspend fun saveOrUpdateTaskInDb(uid: String, taskName: String, courseName: String, deadline: LocalDateTime?, body: String) {
        println("Saving/Updating task '$taskName' to the database.")
        dbQuery {
            Tasks.upsert(Tasks.userId, Tasks.emailUid) {
                it[Tasks.userId] = this@TaskProcessorService.userId
                it[emailUid] = uid
                it[Tasks.taskName] = taskName
                it[Tasks.courseName] = courseName
                it[Tasks.deadline] = deadline
                it[Tasks.body] = body
            }
        }
    }
}
