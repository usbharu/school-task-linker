package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Tasks
import dev.usbharu.stl.model.toTaskDetail
import dev.usbharu.stl.plugins.NotFoundException
import dev.usbharu.stl.plugins.UserSession
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

fun Route.taskRoutes() {
    // 既存のタスク削除ルート
    post("/task/delete") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val taskId = params["id"]?.toIntOrNull()
            ?: throw NotFoundException("Invalid Task ID")

        val deletedCount = dbQuery {
            Tasks.deleteWhere { (Tasks.id eq taskId) and (Tasks.userId eq userSession.userId) }
        }

        if (deletedCount > 0) {
            call.respondRedirect("/dashboard?status=task_deleted")
        } else {
            throw NotFoundException("Task with ID $taskId not found or you don't have permission.")
        }
    }

    // 単一のタスク詳細をJSONで取得するルート
    get("/task/{id}") {
        val userSession = call.principal<UserSession>()!!
        val taskId = call.parameters["id"]?.toIntOrNull()
            ?: throw NotFoundException("Invalid Task ID Format")

        val taskDetail = dbQuery {
            Tasks.selectAll().where { (Tasks.id eq taskId) and (Tasks.userId eq userSession.userId) }
                .map(::toTaskDetail)
                .singleOrNull()
        }

        // taskDetailがnullならNotFoundExceptionをスローする
        if (taskDetail != null) {
            call.respond(taskDetail)
        } else {
            throw NotFoundException("Task with ID $taskId not found.")
        }
    }
}
