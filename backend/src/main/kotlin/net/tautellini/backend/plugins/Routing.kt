package net.tautellini.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.services.FirestoreService

fun Application.configureRouting(firestoreService: FirestoreService) {
    routing {
        // Health check — no rate limit (used by Cloud Run)
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        rateLimit(RateLimitName("api")) {
            route("/api/v1") {
                // Add your project routes here, e.g.:
                // route("/arena-tactics") { ... }
                // route("/project2") { ... }

                get {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "API v1"))
                }
            }
        }
    }
}
