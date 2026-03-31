package net.tautellini.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.routes.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.auth.JwtService
import net.tautellini.backend.auth.UserService
import net.tautellini.backend.auth.oauthRoutes
import net.tautellini.backend.auth.userRoutes

fun Application.configureRouting(
    firestoreService: FirestoreService,
    jwtService: JwtService,
    userService: UserService
) {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        // OAuth routes (public, no rate limit — has its own CSRF protection)
        oauthRoutes(jwtService, userService)

        rateLimit(RateLimitName("api")) {
            route("/api/v1") {
                get {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "API v1"))
                }

                route("/arena-tactics") {
                    addonRoutes(firestoreService)
                    gameModeRoutes(firestoreService)
                    specRoutes(firestoreService)
                    compositionRoutes(firestoreService)
                    matchupRoutes(firestoreService)
                    gearRoutes(firestoreService)
                    ladderRoutes(firestoreService)
                    specMetaRoutes(firestoreService)
                }

                // User routes (JWT authenticated)
                userRoutes(jwtService, userService)
            }
        }

        rateLimit(RateLimitName("write")) {
            route("/api/v1") {
                adminRoutes(firestoreService)
            }
        }
    }
}
