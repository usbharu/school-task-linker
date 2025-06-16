package dev.usbharu.stl.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            // JSONの出力を整形する（開発時に見やすくなります）
            prettyPrint = true
            // 不明なJSONキーを無視する
            isLenient = true
        })
    }
}
