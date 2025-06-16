package dev.usbharu.stl.service

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.MailSettings
import dev.usbharu.stl.model.RegexRules
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.toRegexRule
import jakarta.mail.*
import jakarta.mail.internet.MimeUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.pop3.POP3Folder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.util.*
import kotlin.math.max

/**
 * メールサーバーからメールを取得し、処理するサービス
 */
class MailReaderService(private val userId: Int) {

    data class SimpleEmail(val uid: String, val subject: String, val body: String)

    suspend fun checkAndProcessEmails() {
        val mailSettings = dbQuery {
            MailSettings.select { MailSettings.userId eq userId }.singleOrNull()
        } ?: return

        val regexRules = dbQuery {
            RegexRules.select { RegexRules.userId eq userId }.map(::toRegexRule)
        }

        if (regexRules.isEmpty()) return

        // Google連携が有効かどうかのフラグを取得
        val isGoogleConnected = dbQuery {
            TodoServices.select { (TodoServices.userId eq userId) and (TodoServices.serviceName eq "GoogleTasks") }.any()
        }

        val emails = fetchEmails(
            host = mailSettings[MailSettings.host],
            port = mailSettings[MailSettings.port],
            email = mailSettings[MailSettings.email],
            password = mailSettings[MailSettings.password]
        )

        TaskProcessorService(userId, regexRules, isGoogleConnected).process(emails)
    }

    private suspend fun fetchEmails(host: String, port: Int, email: String, password: String): List<SimpleEmail> {
        return withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.pop3s.host", host)
                put("mail.pop3s.port", port.toString())
                put("mail.pop3s.ssl.enable", "true")
            }
            val session = Session.getInstance(props)
            val store = session.getStore("pop3s")

            try {
                store.connect(host, email, password)
                val pop3Folder = store.getFolder("INBOX") as POP3Folder
                pop3Folder.open(Folder.READ_ONLY)

                val totalMessages = pop3Folder.messageCount
                if (totalMessages == 0) {
                    pop3Folder.close(false)
                    return@withContext emptyList()
                }

                val start = max(1, totalMessages - 9)
                val messages = pop3Folder.getMessages(start, totalMessages)

                val simpleEmails = messages.reversed().mapNotNull { message ->
                    try {
                        val subject = MimeUtility.decodeText(message.subject)
                        val body = getTextFromPart(message)
                        val uid = pop3Folder.getUID(message)

                        SimpleEmail(uid, subject, body)
                    } catch (e: Exception) {
                        println("Error parsing one email: ${e.message}")
                        null
                    }
                }

                pop3Folder.close(false)
                simpleEmails
            } catch(e: Exception) {
                println("Error fetching email: ${e.message}")
                emptyList()
            } finally {
                if (store.isConnected) {
                    store.close()
                }
            }
        }
    }

    private fun getTextFromPart(part: Part): String {
        if (part.content is Multipart) {
            val multipart = part.content as Multipart
            var textContent: String? = null
            var htmlContent: String? = null

            for (i in 0 until multipart.count) {
                val bodyPart = multipart.getBodyPart(i)
                if (bodyPart.isMimeType("text/html")) {
                    htmlContent = getTextFromPart(bodyPart)
                } else if (bodyPart.isMimeType("text/plain")) {
                    textContent = getTextFromPart(bodyPart)
                }
            }
            return htmlContent ?: textContent ?: ""
        }

        var rawText = ""
        if (part.isMimeType("text/html")) {
            val html = part.content.toString()
            val doc = Jsoup.parse(html)

            doc.outputSettings().prettyPrint(false)
            doc.select("br, p").after("\\n")
            doc.select("h1, h2, h3, h4, h5, h6").before("\\n")

            val textWithLineBreaks = Jsoup.clean(doc.html(), "", Safelist.none(), doc.outputSettings())
                .replace("\\\\n", "\n")

            rawText = textWithLineBreaks
        } else if (part.isMimeType("text/plain")) {
            rawText = part.content.toString()
        }

        return rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}
