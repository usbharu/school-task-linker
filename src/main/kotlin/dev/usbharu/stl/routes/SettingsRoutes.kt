package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.GoogleTaskList
import dev.usbharu.stl.model.RegexRules
import dev.usbharu.stl.model.TodoServices
import dev.usbharu.stl.model.toRegexRule
import dev.usbharu.stl.plugins.UserSession
import dev.usbharu.stl.service.GoogleTasksService
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

fun Route.settingsRoutes() {
    get("/settings") {
        val userSession = call.principal<UserSession>()!!

        val googleServiceInfo = dbQuery {
            TodoServices.selectAll().where {
                (TodoServices.userId eq userSession.userId) and (TodoServices.serviceName eq "GoogleTasks")
            }.singleOrNull()
        }
        val isGoogleConnected = googleServiceInfo != null

        var googleTaskLists: List<GoogleTaskList> = emptyList()
        if (isGoogleConnected) {
            googleTaskLists = GoogleTasksService(userSession.userId).getTaskLists()
        }

        val data = mapOf(
            "user" to userSession,
            "mailSettings" to mapOf<String, Any>(),
            "isGoogleConnected" to isGoogleConnected,
            "googleTaskLists" to googleTaskLists,
            "currentTaskListId" to googleServiceInfo?.get(TodoServices.selectedTaskListId),
            "regexRules" to dbQuery {
                RegexRules.selectAll().where { RegexRules.userId eq userSession.userId }.map(::toRegexRule)
            },
            "status" to call.request.queryParameters["status"],
            "error" to call.request.queryParameters["error"]
        )

        call.respond(FreeMarkerContent("settings.ftl", data))
    }

    post("/settings/mail") {
        call.principal<UserSession>()!!
        call.respondRedirect("/settings?status=mail_saved")
    }
}
