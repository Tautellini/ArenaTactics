package net.tautellini.models.user

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val providers: List<String> = emptyList(),
    val tier: UserTier = UserTier.FREE,
    val createdAt: String = "",
    val dashboardCards: List<DashboardCard> = emptyList()
)

@Serializable
enum class UserTier { FREE, PREMIUM }

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: User
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class TokenResponse(
    val token: String,
    val refreshToken: String
)
