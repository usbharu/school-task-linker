package dev.usbharu.stl.service

import dev.usbharu.stl.model.MailSettings
import dev.usbharu.stl.model.RegexRule
import dev.usbharu.stl.model.RegexRules
import dev.usbharu.stl.model.Sessions
import dev.usbharu.stl.model.Tasks
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.Users
import dev.usbharu.stl.model.Users.passwordHash
import dev.usbharu.stl.model.Users.username
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskProcessorServiceTest {

    private lateinit var database: Database

    /**
     * 各テストの実行前に、インメモリH2データベースをセットアップし、
     * 必要なテーブルを作成します。
     */
    @BeforeEach
    fun setup() {
        database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction(database) {
            SchemaUtils.create(Users, MailSettings, RegexRules, TodoServices, Sessions, Tasks)
        }
    }

    /**
     * 各テストの実行後に、作成したテーブルをすべて削除し、
     * データベース接続を閉じてクリーンアップします。
     */
    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(Users, MailSettings, RegexRules, TodoServices, Sessions, Tasks)
        }
        TransactionManager.closeAndUnregister(database)
    }

    @Test
    fun `process - DEADLINE_NOTICEメールを正しく解析しDBに保存できること`() = runBlocking {
        // --- 準備 (Arrange) ---
        val testUserId = 1
        val testUserEmailUid = "fictional-uid-001"

        transaction(database) {
            Users.upsert(Users.username) {
                it[id] = testUserId
                it[username] = "testuser"
                it[passwordHash] = "dummy"
            }
        }
        val rule = RegexRule(1, "期限通知", "課題の期限が近づいています", "DEADLINE_NOTICE")

        val rawEmailBody = """
            学生番号：123456
            
            架空のプログラミング基礎【A棟】
            
            課題の期限が近づいています
            
            第5回レポート：データ構造
            
            期限 : 2025年8月10日 18:30:00 JST
        """.trimIndent()

        val cleanEmailBody = rawEmailBody.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        val email = MailReaderService.SimpleEmail(
            uid = testUserEmailUid,
            subject = "課題の期限が近づいています",
            body = cleanEmailBody
        )

        val service = TaskProcessorService(testUserId, listOf(rule), isGoogleConnected = false)

        // --- 実行 (Act) ---
        service.process(listOf(email))

        // --- 検証 (Assert) ---
        val savedTask = transaction(database) {
            Tasks.select { Tasks.emailUid eq testUserEmailUid }.singleOrNull()
        }

        assertNotNull(savedTask, "タスクがDBに保存されていません。")
        savedTask!!

        assertEquals("架空のプログラミング基礎【A棟】", savedTask[Tasks.courseName], "講義名が正しく抽出されていません。")
        assertEquals("第5回レポート：データ構造", savedTask[Tasks.taskName], "課題名が正しく抽出されていません。")
        assertEquals(cleanEmailBody, savedTask[Tasks.body], "メール本文が正しく保存されていません。")

        val expectedDeadlineJst = ZonedDateTime.of(2025, 8, 10, 18, 30, 0, 0, ZoneId.of("Asia/Tokyo"))
        val expectedDeadlineUtc = expectedDeadlineJst.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime()
        assertEquals(expectedDeadlineUtc, savedTask[Tasks.deadline], "期限日時がUTCで正しく変換・保存されていません。")
    }

    @Test
    fun `process - 複数のメールを解析しDBに正しく保存できること`() = runBlocking {
        // --- 準備 (Arrange) ---
        val testUserId = 1
        val rule = RegexRule(1, "期限通知", "課題の期限が近づいています", "DEADLINE_NOTICE")

        transaction(database) {
            Users.upsert(Users.username) {
                it[id] = testUserId
                it[username] = "testuser"
                it[passwordHash] = "dummy"
            }
        }

        val email1 = MailReaderService.SimpleEmail(
            uid = "uid-a1", subject = "課題の期限が近づいています",
            body = "情報リテラシー\n課題の期限が近づいています\nレポート1：情報社会について\n期限 : 2025年7月20日 23:59:00"
        )
        val email2 = MailReaderService.SimpleEmail(
            uid = "uid-b2", subject = "課題の期限が近づいています",
            body = "統計学入門\n課題の期限が近づいています\n小テスト3\n期限 : 2025年7月25日 17:00:00"
        )

        val service = TaskProcessorService(testUserId, listOf(rule), isGoogleConnected = false)

        // --- 実行 (Act) ---
        assertDoesNotThrow {
            runBlocking {
                service.process(listOf(email1, email2))
            }
        }

        // --- 検証 (Assert) ---
        val taskCount = transaction(database) {
            Tasks.select { Tasks.userId eq testUserId }.count()
        }
        assertEquals(2, taskCount, "2件のタスクがDBに保存されているはずです。")
    }
}
