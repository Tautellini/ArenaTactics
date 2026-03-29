package net.tautellini.backend.arenatactics.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.models.arenatactics.WowSpec
import net.tautellini.models.arenatactics.WowClass

fun Route.specRoutes(firestore: FirestoreService) {
    route("/spec-pools/{specPoolId}") {
        get {
            val specPoolId = call.parameters["specPoolId"]!!
            val specs = firestore.listSubcollection("spec_pools", specPoolId, "specs") {
                fromFirestore<WowSpec>(it)
            }
            if (specs.isEmpty()) call.respond(HttpStatusCode.NotFound)
            else call.respond(specs)
        }
    }

    route("/class-pools/{classPoolId}") {
        get {
            val classPoolId = call.parameters["classPoolId"]!!
            val classes = firestore.listSubcollection("class_pools", classPoolId, "classes") {
                fromFirestore<WowClass>(it)
            }
            if (classes.isEmpty()) call.respond(HttpStatusCode.NotFound)
            else call.respond(classes)
        }
    }
}
