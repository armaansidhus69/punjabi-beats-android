package com.punjabibeats.app.core.util

import kotlinx.coroutines.CancellationException

/** MVI-style UI state used by every screen in the app. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>
}

inline fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

inline val UiState<*>.isLoading: Boolean get() = this is UiState.Loading

/** Runs [block] converting every failure into a Result, while preserving cancellation. */
suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(t)
}

fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
