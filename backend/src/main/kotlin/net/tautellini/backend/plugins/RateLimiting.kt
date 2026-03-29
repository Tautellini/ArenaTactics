package net.tautellini.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiting() {
    install(RateLimit) {
        // Default rate limit for API routes: 60 requests per minute per IP
        register(RateLimitName("api")) {
            rateLimiter(limit = 60, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }

        // Stricter limit for write operations: 10 requests per minute per IP
        register(RateLimitName("write")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.local.remoteAddress
            }
        }
    }
}
