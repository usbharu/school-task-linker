package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Tasks
import dev.usbharu.stl.model.toTaskDetail
import dev.usbharu.stl.plugins.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select

fun Route.taskRoutes() {
    // 既存のタスク削除ルート
    post("/task/delete") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val taskId = params["id"]?.toIntOrNull()
            ?: return@post call.respondRedirect("/dashboard?error=invalid_task_id")

        val deletedCount = dbQuery {
            Tasks.deleteWhere { (Tasks.id eq taskId) and (Tasks.userId eq userSession.userId) }
        }

        if (deletedCount > 0) {
            call.respondRedirect("/dashboard?status=task_deleted")
        } else {
            call.respondRedirect("/dashboard?error=task_delete_failed")
        }
    }

    // 新しいルート：単一のタスク詳細をJSONで取得する
    get("/task/{id}") {
        val userSession = call.principal<UserSession>()!!
        val taskId = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid Task ID")

        val taskDetail = dbQuery {
            Tasks.select { (Tasks.id eq taskId) and (Tasks.userId eq userSession.userId) }
                .map(::toTaskDetail)
                .singleOrNull()
        }

        if (taskDetail != null) {
            call.respond(taskDetail)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
