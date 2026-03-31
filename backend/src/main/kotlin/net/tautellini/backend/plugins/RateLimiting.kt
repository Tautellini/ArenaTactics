package net.tautellini.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiting() {
    install(RateLimit) {
        // Default rate limit for API routes: 300 requests per minute per IP
        register(RateLimitName("api")) {
            rateLimiter(limit = 300, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }

        // Stricter limit for write operations: 30 requests per minute per IP
        register(RateLimitName("write")) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }
    }
}
