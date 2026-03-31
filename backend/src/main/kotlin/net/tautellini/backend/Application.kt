package net.tautellini.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.tautellini.backend.auth.JwtService
import net.tautellini.backend.auth.UserService
import net.tautellini.backend.plugins.configureCompression
import net.tautellini.backend.plugins.configureRouting
import net.tautellini.backend.plugins.configureSerialization
import net.tautellini.backend.plugins.configureCors
import net.tautellini.backend.plugins.configureStatusPages
import net.tautellini.backend.plugins.configureAuth
import net.tautellini.backend.plugins.configureRateLimiting
import net.tautellini.backend.arenatactics.services.FirestoreService

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val firestoreService = FirestoreService()
    val jwtService = JwtService()
    val userService = UserService(firestoreService)

    embeddedServer(Netty, port = port) {
        configureSerialization()
        configureCompression()
        configureCors()
        configureStatusPages()
        configureAuth(jwtService)
        configureRateLimiting()
        configureRouting(firestoreService, jwtService, userService)
    }.start(wait = true)
}
