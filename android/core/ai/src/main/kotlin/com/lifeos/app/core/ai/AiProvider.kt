package com.lifeos.app.core.ai

import com.lifeos.app.core.common.Outcome
import java.io.File

/**
 * Swappable AI brain contract (Memory §17 / Architecture §10's "Stateless
 * AI" pattern) — callers depend on this interface only, never on a specific
 * provider, so a decade-long product isn't hard-tied to one vendor/model.
 * v1 ships exactly one implementation, [GeminiAiProvider]; a circuit
 * breaker/multi-provider fallback (Master Plan §8) is a deliberate, later
 * addition this seam makes possible without touching call sites.
 */
interface AiProvider {
    suspend fun transcribe(audio: File): Outcome<String>

    suspend fun understand(request: UnderstandRequest): Outcome<AiIntent>

    /**
     * A cheap auth/connectivity check — must NOT spend a billed model
     * completion just to confirm a key works (that's what a naive
     * `understand()`-based "test connection" button would do).
     */
    suspend fun verifyKey(): Outcome<Unit>
}
