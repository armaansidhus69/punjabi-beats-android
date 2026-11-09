package com.punjabibeats.app.data.cipher

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the PoToken (BotGuard attestation token) for web-client player
 * requests, following the zemer-cipher approach of keeping the token OUT of
 * the binary: BotGuard challenges require a live browser environment and
 * rotate constantly, so Punjabi Beats does not generate tokens itself. Instead
 * the operator may provision a content-binding token through the remote
 * [CipherConfig]; when none is present the app plays unattested web requests
 * and falls back through the resolver chain if YouTube rejects them.
 */
@Singleton
class PoTokenProvider @Inject constructor(
    private val configStore: PlayerConfigStore,
) {

    /** Returns the configured PoToken, or null when none is provisioned. */
    suspend fun poToken(): String? =
        runCatching { configStore.config().poToken }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    /** True when the current player spec demands a PoToken for playback. */
    suspend fun required(): Boolean = runCatching {
        configStore.config().latestPlayer()?.requiresPoToken == true
    }.getOrDefault(false)
}
