package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.Addon

fun Route.addonRoutes(firestore: FirestoreService) {
    route("/addons") {
        get {
            val addons = firestore.listDocuments("addons") { fromFirestore<Addon>(it) }
            call.respond(addons)
        }
        get("/{addonId}") {
            val addonId = call.parameters["addonId"]!!
            val addon = firestore.getDocument("addons", addonId) { fromFirestore<Addon>(it) }
            if (addon != null) call.respond(addon) else call.respond(HttpStatusCode.NotFound)
        }
    }
}
