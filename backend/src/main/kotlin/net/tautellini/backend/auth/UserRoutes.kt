package net.tautellini.backend.auth

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.plugins.UserPrincipal
import net.tautellini.models.user.DashboardCard
import net.tautellini.models.user.RefreshRequest
import net.tautellini.models.user.TokenResponse

fun Route.userRoutes(jwtService: JwtService, userService: UserService) {
    // Token refresh (public — uses refresh token, not JWT)
    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()
        val userId = userService.validateAndRotateRefreshToken(request.refreshToken)
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired refresh token"))
            return@post
        }

        val user = userService.getUser(userId)
        if (user == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            return@post
        }

        val newJwt = jwtService.sign(user.uid, user.email, user.displayName)
        val newRefresh = userService.createRefreshToken(user.uid)
        call.respond(TokenResponse(token = newJwt, refreshToken = newRefresh))
    }

    // Protected user endpoints
    authenticate("user") {
        route("/api/v1/user") {
            get("/me") {
                val principal = call.principal<UserPrincipal>()!!
                val user = userService.getUser(principal.uid)
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }

            put("/me/dashboard") {
                val principal = call.principal<UserPrincipal>()!!
                val cards = call.receive<List<DashboardCard>>()
                userService.updateDashboardCards(principal.uid, cards)
                call.respond(HttpStatusCode.OK, mapOf("updated" to cards.size))
            }
        }
    }
}
