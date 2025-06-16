package dev.usbharu.stl.service

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.RegexRule
import dev.usbharu.stl.model.Tasks
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class TaskProcessorService(
    private val userId: Int,
    private val rules: List<RegexRule>,
    private val isGoogleConnected: Boolean // Google連携済みかどうかのフラグ
) {
    suspend fun process(emails: List<MailReaderService.SimpleEmail>) {
        if (emails.isEmpty()) return
        println("Processing ${emails.size} emails for user $userId...")

        for (email in emails) {
            for (rule in rules) {
                val regex = Regex(rule.pattern)
                if (regex.matches(email.subject)) {
                    val match = regex.find(email.subject)!!
                    val taskNameFromSubject = if (match.groupValues.size > 1) match.groupValues[1].trim() else match.value.trim()

                    when (rule.category) {
                        "NEW_ASSIGNMENT" -> processNewAssignment(taskNameFromSubject, email)
                        "DEADLINE_NOTICE" -> processDeadlineNotice(taskNameFromSubject, email)
                        "EVENT" -> processEvent(taskNameFromSubject, email)
                        "OTHER" -> processOther(taskNameFromSubject, email)
                    }
                    break
                }
            }
        }
    }

    private suspend fun processNewAssignment(taskName: String, email: MailReaderService.SimpleEmail) {
        println("[Processor] processNewAssignment is not fully implemented yet.")
    }

    private suspend fun processDeadlineNotice(taskNameFromSubject: String, email: MailReaderService.SimpleEmail) {
        println("==============================================")
        println("[Processor] Category: DEADLINE_NOTICE for email UID: ${email.uid}")

        val bodyLines = email.body.lines()

        var courseName = "（講義名不明）"
        val noticeLineIndex = bodyLines.indexOfFirst { it.contains("課題の期限が近づいています") }
        if (noticeLineIndex > 0) {
            courseName = bodyLines[noticeLineIndex - 1]
        }
        println("Extracted Course Name: $courseName")

        var finalTaskName = taskNameFromSubject
        if (noticeLineIndex != -1 && noticeLineIndex + 1 < bodyLines.size) {
            finalTaskName = bodyLines[noticeLineIndex + 1]
        }
        println("Final Task Name: $finalTaskName")

        val deadlinePattern = Regex("""期限\s*[:：]\s*(\d{4}年\d{1,2}月\d{1,2}日\s*\d{1,2}:\d{2}:\d{2})""")
        val deadlineMatch = deadlinePattern.find(email.body)
        var deadline: LocalDateTime? = null
        var deadlineZoned: ZonedDateTime? = null
        if (deadlineMatch != null) {
            val deadlineString = deadlineMatch.groupValues[1].trim()
            println("Deadline Text: $deadlineString")
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日 H:mm:ss", Locale.JAPANESE)
                val localDateTime = LocalDateTime.parse(deadlineString, formatter)

                val jstZone = ZoneId.of("Asia/Tokyo")
                deadlineZoned = ZonedDateTime.of(localDateTime, jstZone)
                deadline = deadlineZoned.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
                println("Parsed Deadline (UTC): $deadline")
            } catch (e: DateTimeParseException) {
                println("Failed to parse deadline: '$deadlineString'. Error: ${e.message}")
            }
        } else {
            println("Deadline Text: Not found in body")
        }

        saveOrUpdateTask(email.uid, finalTaskName, courseName, deadline, email.body)

        // --- Google ToDoリストとの同期 ---
        if (isGoogleConnected) {
            println("Syncing task with Google ToDo...")
            val googleTasksService = GoogleTasksService(this.userId)
            val notesBody = """
                講義名: $courseName
                
                --- 元のメール本文 ---
                ${email.body}
            """.trimIndent()

            googleTasksService.createTask(
                taskTitle = "[$courseName] $finalTaskName",
                notes = notesBody,
                due = deadlineZoned // タイムゾーン情報付きの期限を渡す
            )
        }

        println("==============================================")
    }

    private fun processEvent(eventName: String, email: MailReaderService.SimpleEmail) {
        println("[Processor] processEvent is not implemented yet.")
    }

    private fun processOther(taskName: String, email: MailReaderService.SimpleEmail) {
        println("[Processor] processOther is not implemented yet.")
    }

    private suspend fun saveOrUpdateTask(
        uid: String,
        taskName: String,
        courseName: String,
        deadline: LocalDateTime?,
        body: String
    ) {
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
        println("Saved/Updated task '$taskName' to the database.")
    }
}
