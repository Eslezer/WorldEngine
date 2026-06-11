package com.example.worldengine.core.util

/** Minimal result wrapper for operations that can fail with a user-facing message. */
sealed interface GenResult<out T> {
    data class Success<T>(val data: T) : GenResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : GenResult<Nothing>
}
