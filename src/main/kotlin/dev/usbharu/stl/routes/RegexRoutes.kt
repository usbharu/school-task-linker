package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.RegexRules
import dev.usbharu.stl.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

fun Route.regexRoutes() {
    // 新しい正規表現ルールを追加する
    post("/settings/rules/add") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val name = params["name"] ?: return@post call.respondRedirect("/settings?error=rule_missing_fields#regex-rules")
        val pattern = params["pattern"] ?: return@post call.respondRedirect("/settings?error=rule_missing_fields#regex-rules")
        val category = params["category"] ?: return@post call.respondRedirect("/settings?error=rule_missing_fields#regex-rules")

        dbQuery {
            RegexRules.insert {
                it[userId] = userSession.userId
                it[RegexRules.name] = name
                it[RegexRules.pattern] = pattern
                it[RegexRules.category] = category
            }
        }

        call.respondRedirect("/settings?status=rule_added#regex-rules")
    }

    // 既存の正規表現ルールを更新する
    post("/settings/rules/update") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val id = params["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/settings?error=rule_invalid_id#regex-rules")
        val name = params["name"] ?: return@post call.respondRedirect("/settings?error=rule_missing_fields#regex-rules")
        val pattern = params["pattern"] ?: return@post call.respondRedirect("/settings?error=rule_missing_fields#regex-rules")
        val category = params["category"] ?: return@post call.respondRedirect("/settings?error=rule_missing_fields#regex-rules")

        dbQuery {
            RegexRules.update({ (RegexRules.id eq id) and (RegexRules.userId eq userSession.userId) }) {
                it[RegexRules.name] = name
                it[RegexRules.pattern] = pattern
                it[RegexRules.category] = category
            }
        }

        call.respondRedirect("/settings?status=rule_updated#regex-rules")
    }

    // 正規表現ルールを削除する
    post("/settings/rules/delete") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val id = params["id"]?.toIntOrNull() ?: return@post call.respondRedirect("/settings?error=rule_invalid_id#regex-rules")

        dbQuery {
            RegexRules.deleteWhere { (RegexRules.id eq id) and (RegexRules.userId eq userSession.userId) }
        }

        call.respondRedirect("/settings?status=rule_deleted#regex-rules")
    }
}
