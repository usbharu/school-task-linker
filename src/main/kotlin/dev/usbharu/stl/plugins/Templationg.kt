package dev.usbharu.stl.plugins

import freemarker.cache.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import java.nio.charset.StandardCharsets

fun Application.configureTemplating() {
    install(FreeMarker) {
        // テンプレートファイル（.ftl）が格納されているディレクトリを指定します。
        // `src/main/resources/templates` を指します。
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")

        // URLエンコーディングやテンプレートの出力に使われる文字コードをUTF-8に指定
        outputEncoding = StandardCharsets.UTF_8.name()
    }
}
