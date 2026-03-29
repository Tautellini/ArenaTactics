package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.ItemTooltipData
import net.tautellini.models.arenatactics.LadderIndex
import net.tautellini.models.arenatactics.LadderSnapshot
import net.tautellini.models.arenatactics.PlayerProfile

fun Route.ladderRoutes(firestore: FirestoreService) {
    route("/ladder/{addonId}") {
        get("/index") {
            val addonId = call.parameters["addonId"]!!
            val index = firestore.listSubcollection("ladder", addonId, "index") {
                fromFirestore<LadderIndex>(it)
            }
            call.respond(index)
        }

        get("/snapshots/{region}/{bracket}") {
            val addonId = call.parameters["addonId"]!!
            val region = call.parameters["region"]!!
            val bracket = call.parameters["bracket"]!!
            val snapshot = firestore.getSubDocument("ladder", addonId, "snapshots", "${region}_$bracket") {
                fromFirestore<LadderSnapshot>(it)
            }
            if (snapshot != null) call.respond(snapshot) else call.respond(HttpStatusCode.NotFound)
        }

        get("/players/{region}/{characterId}") {
            val addonId = call.parameters["addonId"]!!
            val region = call.parameters["region"]!!
            val characterId = call.parameters["characterId"]!!
            val player = firestore.getSubDocument("ladder", addonId, "players", "${region}_$characterId") {
                fromFirestore<PlayerProfile>(it)
            }
            if (player != null) call.respond(player) else call.respond(HttpStatusCode.NotFound)
        }

        get("/items/{itemId}") {
            val addonId = call.parameters["addonId"]!!
            val itemId = call.parameters["itemId"]!!
            // Search both regions
            for (region in listOf("us", "eu")) {
                val item = firestore.getSubDocument("ladder", addonId, "items", "${region}_$itemId") {
                    fromFirestore<ItemTooltipData>(it)
                }
                if (item != null) {
                    call.respond(item)
                    return@get
                }
            }
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
