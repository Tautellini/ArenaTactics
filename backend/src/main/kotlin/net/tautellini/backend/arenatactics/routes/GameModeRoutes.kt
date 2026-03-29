package net.tautellini.backend.arenatactics.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.GameMode

fun Route.gameModeRoutes(firestore: FirestoreService) {
    route("/game-modes") {
        get {
            val addonId = call.request.queryParameters["addonId"]
            val modes = firestore.listDocuments("game_modes") { fromFirestore<GameMode>(it) }
            val result = if (addonId != null) modes.filter { it.addonId == addonId } else modes
            call.respond(result)
        }
    }
}
