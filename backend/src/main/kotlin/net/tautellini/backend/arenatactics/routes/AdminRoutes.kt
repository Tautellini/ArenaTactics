package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.toFirestore
import net.tautellini.models.arenatactics.*

fun Route.adminRoutes(firestore: FirestoreService) {
    authenticate("admin") {
        route("/admin/arena-tactics") {

            // PUT /addons — upsert list of addons
            put("/addons") {
                val addons = call.receive<List<Addon>>()
                addons.forEach { firestore.setDocument("addons", it.id, toFirestore(it)) }
                call.respond(HttpStatusCode.OK, mapOf("imported" to addons.size))
            }

            // DELETE /addons/{addonId}
            delete("/addons/{addonId}") {
                firestore.deleteDocument("addons", call.parameters["addonId"]!!)
                call.respond(HttpStatusCode.OK, mapOf("deleted" to call.parameters["addonId"]))
            }

            // PUT /game-modes — upsert list of game modes
            put("/game-modes") {
                val modes = call.receive<List<GameMode>>()
                modes.forEach { firestore.setDocument("game_modes", it.id, toFirestore(it)) }
                call.respond(HttpStatusCode.OK, mapOf("imported" to modes.size))
            }

            // PUT /spec-pools/{specPoolId} — upsert specs in a pool
            put("/spec-pools/{specPoolId}") {
                val poolId = call.parameters["specPoolId"]!!
                val specs = call.receive<List<WowSpec>>()
                firestore.setDocument("spec_pools", poolId, mapOf("id" to poolId))
                specs.forEach { firestore.setSubDocument("spec_pools", poolId, "specs", it.id, toFirestore(it)) }
                call.respond(HttpStatusCode.OK, mapOf("imported" to specs.size))
            }

            // PUT /class-pools/{classPoolId} — upsert classes in a pool
            put("/class-pools/{classPoolId}") {
                val poolId = call.parameters["classPoolId"]!!
                val classes = call.receive<List<WowClass>>()
                firestore.setDocument("class_pools", poolId, mapOf("id" to poolId))
                classes.forEach { firestore.setSubDocument("class_pools", poolId, "classes", it.id, toFirestore(it)) }
                call.respond(HttpStatusCode.OK, mapOf("imported" to classes.size))
            }

            // PUT /composition-sets/{setId} — upsert compositions
            put("/composition-sets/{setId}") {
                val setId = call.parameters["setId"]!!
                val compositions = call.receive<List<Composition>>()
                firestore.setDocument("composition_sets", setId, mapOf("id" to setId))
                compositions.forEach { firestore.setSubDocument("composition_sets", setId, "compositions", it.id, toFirestore(it)) }
                call.respond(HttpStatusCode.OK, mapOf("imported" to compositions.size))
            }

            // PUT /matchups/{compositionId} — upsert matchups for a composition
            put("/matchups/{compositionId}") {
                val compId = call.parameters["compositionId"]!!
                val matchups = call.receive<List<Matchup>>()
                firestore.setDocument("matchups", compId, mapOf("id" to compId))
                matchups.forEach { firestore.setSubDocument("matchups", compId, "entries", it.id, toFirestore(it)) }
                call.respond(HttpStatusCode.OK, mapOf("imported" to matchups.size))
            }

            // PUT /gear/{classId}/phase/{phase} — upsert a gear phase
            put("/gear/{classId}/phase/{phase}") {
                val classId = call.parameters["classId"]!!
                val phase = call.parameters["phase"]!!
                val gearPhase = call.receive<GearPhase>()
                firestore.setDocument("gear", "${classId}_phase$phase", toFirestore(gearPhase))
                call.respond(HttpStatusCode.OK, mapOf("imported" to "${classId}_phase$phase"))
            }

            // PUT /ladder/{addonId}/snapshot — upsert a ladder snapshot + auto-update index
            put("/ladder/{addonId}/snapshot") {
                val addonId = call.parameters["addonId"]!!
                val snapshot = call.receive<LadderSnapshot>()
                firestore.setDocument("ladder", addonId, mapOf("id" to addonId))
                val docId = "${snapshot.region}_${snapshot.bracket}"
                firestore.setSubDocument("ladder", addonId, "snapshots", docId, toFirestore(snapshot))
                val index = LadderIndex(snapshot.region, snapshot.bracket, "$docId.json")
                firestore.setSubDocument("ladder", addonId, "index", docId, toFirestore(index))
                call.respond(HttpStatusCode.OK, mapOf("imported" to docId))
            }

            // PUT /ladder/{addonId}/players/{region} — upsert player profiles
            put("/ladder/{addonId}/players/{region}") {
                val addonId = call.parameters["addonId"]!!
                val region = call.parameters["region"]!!
                val players = call.receive<Map<String, PlayerProfile>>()
                firestore.setDocument("ladder", addonId, mapOf("id" to addonId))
                players.forEach { (charId, profile) ->
                    firestore.setSubDocument("ladder", addonId, "players", "${region}_$charId", toFirestore(profile))
                }
                call.respond(HttpStatusCode.OK, mapOf("imported" to players.size))
            }

            // PUT /ladder/{addonId}/items/{region} — upsert item tooltips
            put("/ladder/{addonId}/items/{region}") {
                val addonId = call.parameters["addonId"]!!
                val region = call.parameters["region"]!!
                val items = call.receive<Map<String, ItemTooltipData>>()
                firestore.setDocument("ladder", addonId, mapOf("id" to addonId))
                items.forEach { (itemId, item) ->
                    firestore.setSubDocument("ladder", addonId, "items", "${region}_$itemId", toFirestore(item))
                }
                call.respond(HttpStatusCode.OK, mapOf("imported" to items.size))
            }

            // PUT /spec-meta/{specId} — upsert precomputed spec meta
            put("/spec-meta/{specId}") {
                val specId = call.parameters["specId"]!!
                val meta = call.receive<SpecMeta>()
                firestore.setDocument("spec_meta", specId, toFirestore(meta))
                call.respond(HttpStatusCode.OK, mapOf("imported" to specId))
            }
        }
    }
}
