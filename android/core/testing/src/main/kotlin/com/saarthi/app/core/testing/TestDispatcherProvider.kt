package com.saarthi.app.core.testing

import com.saarthi.app.core.common.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Every future module that injects [DispatcherProvider] uses this in
 * tests — one dispatcher for main/io/default so coroutines launched in
 * a ViewModel under test run synchronously and deterministically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider : DispatcherProvider {
    private val dispatcher = UnconfinedTestDispatcher()
    override val main = dispatcher
    override val io = dispatcher
    override val default = dispatcher
}
