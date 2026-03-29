package net.tautellini.backend.arenatactics.services

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreService(
    private val projectNamespace: String = "arenatactics",
    private val databaseId: String = System.getenv("FIRESTORE_DATABASE") ?: "tautellini"
) {
    val db: Firestore by lazy {
        FirestoreOptions.newBuilder()
            .setDatabaseId(databaseId)
            .build()
            .service
    }

    private fun ref(collection: String) =
        db.collection("projects").document(projectNamespace).collection(collection)

    suspend fun <T> getDocument(collection: String, documentId: String, transform: (Map<String, Any>) -> T): T? =
        withContext(Dispatchers.IO) {
            val doc = ref(collection).document(documentId).get().get()
            if (doc.exists()) doc.data?.let(transform) else null
        }

    suspend fun <T> listDocuments(collection: String, transform: (Map<String, Any>) -> T): List<T> =
        withContext(Dispatchers.IO) {
            ref(collection).get().get().documents.mapNotNull { it.data?.let(transform) }
        }

    suspend fun <T> listSubcollection(
        collection: String,
        documentId: String,
        subcollection: String,
        transform: (Map<String, Any>) -> T
    ): List<T> = withContext(Dispatchers.IO) {
        ref(collection).document(documentId).collection(subcollection)
            .get().get().documents.mapNotNull { it.data?.let(transform) }
    }

    suspend fun <T> getSubDocument(
        collection: String,
        documentId: String,
        subcollection: String,
        subDocumentId: String,
        transform: (Map<String, Any>) -> T
    ): T? = withContext(Dispatchers.IO) {
        val doc = ref(collection).document(documentId).collection(subcollection)
            .document(subDocumentId).get().get()
        if (doc.exists()) doc.data?.let(transform) else null
    }

    suspend fun setDocument(collection: String, documentId: String, data: Map<String, Any>) =
        withContext(Dispatchers.IO) {
            ref(collection).document(documentId).set(data).get()
        }

    suspend fun setSubDocument(
        collection: String,
        documentId: String,
        subcollection: String,
        subDocumentId: String,
        data: Map<String, Any>
    ) = withContext(Dispatchers.IO) {
        ref(collection).document(documentId).collection(subcollection)
            .document(subDocumentId).set(data).get()
    }

    suspend fun deleteDocument(collection: String, documentId: String) =
        withContext(Dispatchers.IO) {
            ref(collection).document(documentId).delete().get()
        }
}
