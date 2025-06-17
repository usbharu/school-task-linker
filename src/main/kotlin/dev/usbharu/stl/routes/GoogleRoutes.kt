package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.TodoServices.selectedTaskListId
import dev.usbharu.stl.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update

fun Route.googleRoutes() {
    // 連携を解除する
    post("/settings/google/disconnect") {
        val userSession = call.principal<UserSession>()!!
        dbQuery {
            TodoServices.deleteWhere {
                (TodoServices.userId eq userSession.userId) and (TodoServices.serviceName eq "GoogleTasks")
            }
        }
        call.respondRedirect("/settings?status=google_disconnected#todo-integration")
    }

    // ToDoの追加先リストを保存する
    post("/settings/google/savelist") {
        val userSession = call.principal<UserSession>()!!
        val params = call.receiveParameters()
        val taskListId = params["taskListId"] ?: return@post call.respondRedirect("/settings?error=google_nolist#todo-integration")

        dbQuery {
            TodoServices.update({ (TodoServices.userId eq userSession.userId) and (TodoServices.serviceName eq "GoogleTasks") }) {
                it[selectedTaskListId] = taskListId
            }
        }
        call.respondRedirect("/settings?status=google_list_saved#todo-integration")
    }
}
