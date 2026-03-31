package net.tautellini.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import net.tautellini.backend.auth.JwtService

data class UserPrincipal(val uid: String, val email: String?) : Principal

fun Application.configureAuth(jwtService: JwtService) {
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

        bearer("user") {
            realm = "User API"
            authenticate { credential ->
                val payload = jwtService.verify(credential.token)
                if (payload != null) {
                    UserPrincipal(payload.uid, payload.email)
                } else {
                    null
                }
            }
        }
    }
}
