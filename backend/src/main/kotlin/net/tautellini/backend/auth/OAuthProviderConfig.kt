package net.tautellini.backend.auth

data class ProviderProfile(
    val email: String?,
    val emailVerified: Boolean,
    val displayName: String?,
    val avatarUrl: String?,
    val providerId: String  // e.g., "google", "discord", "battlenet"
)

data class OAuthProviderConfig(
    val name: String,
    val authUrl: String,
    val tokenUrl: String,
    val profileUrl: String,
    val scopes: String,
    val clientIdEnv: String,
    val clientSecretEnv: String,
    val parseProfile: (Map<String, Any?>) -> ProviderProfile
) {
    val clientId: String get() = System.getenv(clientIdEnv) ?: ""
    val clientSecret: String get() = System.getenv(clientSecretEnv) ?: ""

    companion object {
        val GOOGLE = OAuthProviderConfig(
            name = "google",
            authUrl = "https://accounts.google.com/o/oauth2/v2/auth",
            tokenUrl = "https://oauth2.googleapis.com/token",
            profileUrl = "https://www.googleapis.com/oauth2/v3/userinfo",
            scopes = "openid email profile",
            clientIdEnv = "GOOGLE_OAUTH_CLIENT_ID",
            clientSecretEnv = "GOOGLE_OAUTH_CLIENT_SECRET",
            parseProfile = { data ->
                ProviderProfile(
                    email = data["email"] as? String,
                    emailVerified = data["email_verified"] as? Boolean ?: false,
                    displayName = data["name"] as? String,
                    avatarUrl = data["picture"] as? String,
                    providerId = "google"
                )
            }
        )

        val DISCORD = OAuthProviderConfig(
            name = "discord",
            authUrl = "https://discord.com/api/oauth2/authorize",
            tokenUrl = "https://discord.com/api/oauth2/token",
            profileUrl = "https://discord.com/api/users/@me",
            scopes = "identify email",
            clientIdEnv = "DISCORD_CLIENT_ID",
            clientSecretEnv = "DISCORD_CLIENT_SECRET",
            parseProfile = { data ->
                val id = data["id"] as? String
                val avatar = data["avatar"] as? String
                ProviderProfile(
                    email = data["email"] as? String,
                    emailVerified = data["verified"] as? Boolean ?: false,
                    displayName = data["global_name"] as? String ?: data["username"] as? String,
                    avatarUrl = if (id != null && avatar != null) "https://cdn.discordapp.com/avatars/$id/$avatar.png" else null,
                    providerId = "discord"
                )
            }
        )

        val BATTLENET = OAuthProviderConfig(
            name = "battlenet",
            authUrl = "https://oauth.battle.net/authorize",
            tokenUrl = "https://oauth.battle.net/token",
            profileUrl = "https://oauth.battle.net/userinfo",
            scopes = "openid",
            clientIdEnv = "BATTLENET_CLIENT_ID",
            clientSecretEnv = "BATTLENET_CLIENT_SECRET",
            parseProfile = { data ->
                ProviderProfile(
                    email = data["email"] as? String,
                    emailVerified = true, // Battle.net always verifies emails
                    displayName = data["battletag"] as? String,
                    avatarUrl = null,
                    providerId = "battlenet"
                )
            }
        )

        val ALL = mapOf("google" to GOOGLE, "discord" to DISCORD, "battlenet" to BATTLENET)
    }
}
