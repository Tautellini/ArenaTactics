package net.tautellini.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuth() {
    val adminKey = System.getenv("ADMIN_API_KEY") ?: ""

    install(Authentication) {
        bearer("admin") {
            realm = "Admin API"
            authenticate { credential ->
                if (adminKey.isNotBlank() && credential.token == adminKey) {
                    UserIdPrincipal("admin")
                } else {
                    null
                }
            }
        }
    }
}
