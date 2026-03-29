package net.tautellini.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.tautellini.backend.plugins.configureCompression
import net.tautellini.backend.plugins.configureRouting
import net.tautellini.backend.plugins.configureSerialization
import net.tautellini.backend.plugins.configureCors
import net.tautellini.backend.plugins.configureStatusPages
import net.tautellini.backend.plugins.configureRateLimiting
import net.tautellini.backend.arenatactics.services.FirestoreService

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val firestoreService = FirestoreService()

    embeddedServer(Netty, port = port) {
        configureSerialization()
        configureCompression()
        configureCors()
        configureStatusPages()
        configureRateLimiting()
        configureRouting(firestoreService)
    }.start(wait = true)
}
