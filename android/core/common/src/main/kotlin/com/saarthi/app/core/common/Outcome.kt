package com.saarthi.app.core.common

/**
 * A Result-like wrapper used at every module boundary in the Clean
 * Architecture layering (Engineering Master Plan §5's API contracts).
 * Distinguishes "loading" explicitly rather than overloading null.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: Throwable) : Outcome<Nothing>
    data object Loading : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
    Outcome.Loading -> Outcome.Loading
}
