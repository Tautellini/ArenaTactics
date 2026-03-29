package net.tautellini.backend.arenatactics.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.GearPhase

private const val MAX_PHASES = 5

fun Route.gearRoutes(firestore: FirestoreService) {
    route("/gear/{classId}") {
        get {
            val classId = call.parameters["classId"]!!
            val phases = mutableListOf<GearPhase>()
            for (phase in 1..MAX_PHASES) {
                val gearPhase = firestore.getDocument("gear", "${classId}_phase$phase") {
                    fromFirestore<GearPhase>(it)
                } ?: break
                phases.add(gearPhase)
            }
            call.respond(phases)
        }
    }
}
