package net.tautellini.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.Date

data class JwtPayload(
    val uid: String,
    val email: String?,
    val name: String?
)

class JwtService(
    private val secret: String = System.getenv("JWT_SECRET") ?: error("JWT_SECRET must be set")
) {
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm).withIssuer("arenatactics").build()
    private val expiryMs = 60 * 60 * 1000L // 1 hour

    fun sign(uid: String, email: String?, name: String?): String {
        return JWT.create()
            .withIssuer("arenatactics")
            .withSubject(uid)
            .withClaim("email", email)
            .withClaim("name", name)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expiryMs))
            .sign(algorithm)
    }

    fun verify(token: String): JwtPayload? {
        return try {
            val decoded = verifier.verify(token)
            JwtPayload(
                uid = decoded.subject,
                email = decoded.getClaim("email").asString(),
                name = decoded.getClaim("name").asString()
            )
        } catch (_: JWTVerificationException) {
            null
        }
    }
}
