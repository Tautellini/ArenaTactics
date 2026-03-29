package net.tautellini.backend.services

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

class FirestoreService {
    val db: Firestore by lazy {
        FirestoreOptions.getDefaultInstance().service
    }

    suspend fun getDocument(collection: String, documentId: String): Map<String, Any>? {
        val doc = db.collection(collection).document(documentId).get().get()
        return if (doc.exists()) doc.data else null
    }

    suspend fun setDocument(collection: String, documentId: String, data: Map<String, Any>) {
        db.collection(collection).document(documentId).set(data).get()
    }

    suspend fun deleteDocument(collection: String, documentId: String) {
        db.collection(collection).document(documentId).delete().get()
    }

    suspend fun listDocuments(collection: String): List<Map<String, Any>> {
        return db.collection(collection).get().get().documents.mapNotNull { it.data }
    }
}
