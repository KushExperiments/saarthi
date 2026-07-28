package com.saarthi.app.core.testing

import com.saarthi.app.core.common.DispatcherProvider
import kotlinx.coroutines.Dispatchers

/**
 * Every future module that injects [DispatcherProvider] uses this in
 * tests. Resolves main/io/default to whatever [MainDispatcherRule] has
 * currently installed as [Dispatchers.Main], read lazily on each access,
 * rather than a second, independent `UnconfinedTestDispatcher` instance.
 * Two separate Unconfined dispatchers each still run eagerly on their own,
 * but a coroutine on one only resumes a suspended coroutine on the *other*
 * synchronously when they're actually the same object — with two separate
 * instances, `viewModelScope` (Main) and an injected `dispatchers.io` call
 * didn't reliably complete before the very next line in a test asserted.
 */
class TestDispatcherProvider : DispatcherProvider {
    override val main get() = Dispatchers.Main
    override val io get() = Dispatchers.Main
    override val default get() = Dispatchers.Main
}
