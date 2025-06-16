package dev.usbharu.stl.routes

import dev.usbharu.stl.db.DatabaseFactory.dbQuery
import dev.usbharu.stl.model.Tasks
import dev.usbharu.stl.model.toTask
import dev.usbharu.stl.plugins.UserSession
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select

fun Route.dashboardRoutes() {
    get("/dashboard") {
        val userSession = call.principal<UserSession>()!!

        // クエリパラメータから絞り込みとソートの条件を取得
        val filter = call.request.queryParameters["filter"] ?: "all"
        val sort = call.request.queryParameters["sort"] ?: "deadline_asc"
        val selectedCourse = call.request.queryParameters["course"] ?: "all"

        // DBからユーザーのタスクをすべて取得
        val allTasks = dbQuery {
            Tasks.select { Tasks.userId eq userSession.userId }.map(::toTask)
        }

        // 絞り込み用のユニークな講義名リストを作成
        val courseNames = allTasks.map { it.courseName }.distinct().sorted()

        // 絞り込み処理 (ステータス)
        val statusFilteredTasks = when (filter) {
            "overdue" -> allTasks.filter { it.overdue }
            "incomplete" -> allTasks.filter { !it.overdue && it.deadline != null }
            else -> allTasks // "all"
        }

        // 絞り込み処理 (講義名)
        val courseFilteredTasks = if (selectedCourse == "all") {
            statusFilteredTasks
        } else {
            statusFilteredTasks.filter { it.courseName == selectedCourse }
        }

        // ソート処理
        val sortedTasks = when (sort) {
            "deadline_asc" -> courseFilteredTasks.sortedWith(compareBy(nullsLast()) { it.deadline })
            "deadline_desc" -> courseFilteredTasks.sortedWith(compareByDescending(nullsLast()) { it.deadline })
            else -> courseFilteredTasks
        }

        val data = mapOf(
            "user" to userSession,
            "tasks" to sortedTasks,
            "courseNames" to courseNames,
            "currentFilter" to filter,
            "currentSort" to sort,
            "currentCourse" to selectedCourse,
            "status" to call.request.queryParameters["status"],
            "error" to call.request.queryParameters["error"]
        )
        call.respond(FreeMarkerContent("dashboard.ftl", data))
    }
}
