package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.Matchup

fun Route.matchupRoutes(firestore: FirestoreService) {
    route("/matchups/{compositionId}") {
        get {
            val compositionId = call.parameters["compositionId"]!!
            val matchups = firestore.listSubcollection("matchups", compositionId, "entries") {
                fromFirestore<Matchup>(it)
            }
            call.respond(matchups)
        }
        get("/{matchupId}") {
            val compositionId = call.parameters["compositionId"]!!
            val matchupId = call.parameters["matchupId"]!!
            val matchup = firestore.getSubDocument("matchups", compositionId, "entries", matchupId) {
                fromFirestore<Matchup>(it)
            }
            if (matchup != null) call.respond(matchup) else call.respond(HttpStatusCode.NotFound)
        }
    }
}
