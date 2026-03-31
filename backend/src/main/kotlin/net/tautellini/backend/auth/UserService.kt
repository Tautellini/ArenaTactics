package net.tautellini.backend.auth

import net.tautellini.backend.arenatactics.services.FirestoreService
import net.tautellini.backend.arenatactics.services.fromFirestore
import net.tautellini.backend.arenatactics.services.toFirestore
import net.tautellini.models.user.DashboardCard
import net.tautellini.models.user.User
import net.tautellini.models.user.UserTier
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

class UserService(private val firestore: FirestoreService) {
    private val random = SecureRandom()

    suspend fun findOrCreateByEmail(
        email: String?,
        provider: String,
        displayName: String?,
        avatarUrl: String?
    ): User {
        // Try to find existing user by email
        if (email != null) {
            val existing = findByEmail(email)
            if (existing != null) {
                // Add provider if not already linked
                val updatedProviders = if (provider !in existing.providers) {
                    existing.providers + provider
                } else existing.providers

                val updated = existing.copy(
                    providers = updatedProviders,
                    displayName = displayName ?: existing.displayName,
                    avatarUrl = avatarUrl ?: existing.avatarUrl
                )
                firestore.setDocument("users", updated.uid, toFirestore(updated))
                return updated
            }
        }

        // Create new user
        val uid = UUID.randomUUID().toString()
        val user = User(
            uid = uid,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            providers = listOf(provider),
            tier = UserTier.FREE,
            createdAt = Instant.now().toString()
        )
        firestore.setDocument("users", uid, toFirestore(user))
        return user
    }

    suspend fun getUser(uid: String): User? {
        return firestore.getDocument("users", uid) { fromFirestore<User>(it) }
    }

    suspend fun updateDashboardCards(uid: String, cards: List<DashboardCard>) {
        val user = getUser(uid) ?: return
        val updated = user.copy(dashboardCards = cards)
        firestore.setDocument("users", uid, toFirestore(updated))
    }

    suspend fun createRefreshToken(userId: String): String {
        val token = generateSecureToken()
        val expiresAt = Instant.now().plusSeconds(30L * 24 * 60 * 60).toString() // 30 days
        firestore.setDocument("refresh_tokens", token, mapOf(
            "userId" to userId,
            "expiresAt" to expiresAt
        ))
        return token
    }

    suspend fun validateAndRotateRefreshToken(token: String): String? {
        val doc = firestore.getDocument("refresh_tokens", token) { it } ?: return null
        val userId = doc["userId"] as? String ?: return null
        val expiresAt = doc["expiresAt"] as? String

        // Check expiry
        if (expiresAt != null) {
            try {
                if (Instant.parse(expiresAt).isBefore(Instant.now())) {
                    firestore.deleteDocument("refresh_tokens", token)
                    return null
                }
            } catch (_: Exception) {}
        }

        // Delete old token (rotation)
        firestore.deleteDocument("refresh_tokens", token)
        return userId
    }

    private suspend fun findByEmail(email: String): User? {
        // List all users and find by email
        // For production, this should use a Firestore query with a where clause
        val users = firestore.listDocuments("users") { fromFirestore<User>(it) }
        return users.find { it.email == email }
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
