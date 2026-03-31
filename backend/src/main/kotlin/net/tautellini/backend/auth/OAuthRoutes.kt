package net.tautellini.backend.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

private val stateStore = ConcurrentHashMap<String, StateData>()
private val random = SecureRandom()
private val BACKEND_PUBLIC_URL = System.getenv("BACKEND_PUBLIC_URL") ?: "https://backend-532845578761.europe-west3.run.app"

private data class StateData(
    val redirectUri: String,
    val provider: String,
    val createdAt: Long = System.currentTimeMillis()
)

private val httpClient = HttpClient(io.ktor.client.engine.java.Java) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// Allowed redirect URI prefixes for security
private val ALLOWED_REDIRECT_PREFIXES = listOf(
    "http://localhost",
    "https://tautellini.github.io",
    "https://backend-532845578761.europe-west3.run.app"
)

fun Route.oauthRoutes(jwtService: JwtService, userService: UserService) {
    route("/auth/oauth") {
        get("/{provider}/start") {
            val providerName = call.parameters["provider"]!!
            val provider = OAuthProviderConfig.ALL[providerName]
            if (provider == null || provider.clientId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Unknown or unconfigured provider: $providerName"))
                return@get
            }

            val redirectUri = call.request.queryParameters["redirect_uri"]
            if (redirectUri == null || ALLOWED_REDIRECT_PREFIXES.none { redirectUri.startsWith(it) }) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid redirect_uri"))
                return@get
            }

            // Generate state for CSRF protection
            val stateBytes = ByteArray(24)
            random.nextBytes(stateBytes)
            val state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
            stateStore[state] = StateData(redirectUri, providerName)

            // Clean old states (>10 min)
            val cutoff = System.currentTimeMillis() - 10 * 60 * 1000
            stateStore.entries.removeIf { it.value.createdAt < cutoff }

            // Build authorization URL
            val backendCallbackUrl = "$BACKEND_PUBLIC_URL/auth/oauth/$providerName/callback"
            val authUrl = URLBuilder(provider.authUrl).apply {
                parameters.append("client_id", provider.clientId)
                parameters.append("redirect_uri", backendCallbackUrl)
                parameters.append("response_type", "code")
                parameters.append("scope", provider.scopes)
                parameters.append("state", state)
            }.buildString()

            call.respondRedirect(authUrl)
        }

        get("/{provider}/callback") {
            val providerName = call.parameters["provider"]!!
            val provider = OAuthProviderConfig.ALL[providerName]
            if (provider == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Unknown provider"))
                return@get
            }

            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            if (code == null || state == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing code or state"))
                return@get
            }

            // Validate state
            val stateData = stateStore.remove(state)
            if (stateData == null || stateData.provider != providerName) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid state"))
                return@get
            }

            val backendCallbackUrl = "$BACKEND_PUBLIC_URL/auth/oauth/$providerName/callback"

            try {
                // Exchange code for tokens
                val tokenResponse: Map<String, kotlinx.serialization.json.JsonElement> = httpClient.submitForm(
                    url = provider.tokenUrl,
                    formParameters = parameters {
                        append("client_id", provider.clientId)
                        append("client_secret", provider.clientSecret)
                        append("code", code)
                        append("redirect_uri", backendCallbackUrl)
                        append("grant_type", "authorization_code")
                    }
                ).body()

                val accessToken = tokenResponse["access_token"]?.toString()?.trim('"')
                if (accessToken == null) {
                    call.respond(HttpStatusCode.BadGateway, mapOf("error" to "No access token from provider"))
                    return@get
                }

                // Fetch user profile
                val profileJson: kotlinx.serialization.json.JsonObject = httpClient.get(provider.profileUrl) {
                    headers { append(HttpHeaders.Authorization, "Bearer $accessToken") }
                }.body()

                // Convert JsonObject to Map<String, Any?> for the profile parser
                val profileData = profileJson.entries.associate { (k, v) ->
                    k to when (v) {
                        is kotlinx.serialization.json.JsonPrimitive -> {
                            if (v.isString) v.content
                            else v.content.toBooleanStrictOrNull() ?: v.content.toLongOrNull() ?: v.content
                        }
                        else -> v.toString()
                    }
                }

                val profile = provider.parseProfile(profileData)

                // Only link accounts by verified email
                val emailForLinking = if (profile.emailVerified) profile.email else null

                // Find or create user
                val user = userService.findOrCreateByEmail(
                    email = emailForLinking,
                    provider = profile.providerId,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl
                )

                // Sign JWT
                val jwt = jwtService.sign(user.uid, user.email, user.displayName)
                val refreshToken = userService.createRefreshToken(user.uid)

                // Redirect to frontend with tokens
                val separator = if ("?" in stateData.redirectUri) "&" else "?"
                call.respondRedirect("${stateData.redirectUri}${separator}token=$jwt&refresh=$refreshToken")
            } catch (e: Exception) {
                call.application.environment.log.error("OAuth callback error for $providerName", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to "Authentication failed",
                    "detail" to (e.message ?: e.toString())
                ))
            }
        }
    }
}
