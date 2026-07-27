package com.saarthi.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeTest {

    @Test
    fun `map transforms a Success value`() {
        val result = Outcome.Success(2).map { it * 21 }
        assertEquals(Outcome.Success(42), result)
    }

    @Test
    fun `map passes through a Failure unchanged`() {
        val failure = Outcome.Failure(IllegalStateException("boom"))
        val result = failure.map { it }
        assertTrue(result is Outcome.Failure)
    }

    @Test
    fun `map passes through Loading unchanged`() {
        val result: Outcome<Int> = Outcome.Loading
        assertEquals(Outcome.Loading, result.map { it })
    }
}
