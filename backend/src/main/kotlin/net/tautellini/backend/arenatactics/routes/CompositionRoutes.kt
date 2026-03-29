package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.Composition

fun Route.compositionRoutes(firestore: FirestoreService) {
    route("/composition-sets/{compositionSetId}") {
        get {
            val setId = call.parameters["compositionSetId"]!!
            val compositions = firestore.listSubcollection("composition_sets", setId, "compositions") {
                fromFirestore<Composition>(it)
            }
            if (compositions.isEmpty()) call.respond(HttpStatusCode.NotFound)
            else call.respond(compositions)
        }
    }
}
