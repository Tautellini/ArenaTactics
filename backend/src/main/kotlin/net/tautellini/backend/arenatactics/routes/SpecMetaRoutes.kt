package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.SpecMeta

fun Route.specMetaRoutes(firestore: FirestoreService) {
    route("/spec-meta/{specId}") {
        get {
            val specId = call.parameters["specId"]!!
            val meta = firestore.getDocument("spec_meta", specId) { fromFirestore<SpecMeta>(it) }
            if (meta != null) call.respond(meta) else call.respond(HttpStatusCode.NotFound)
        }
    }
}
