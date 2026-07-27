package com.saarthi.app.feature.placeholder

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderViewModelTest {

    @Test
    fun `message comes from the injected GreetingProvider`() {
        val viewModel = PlaceholderViewModel(GreetingProvider())

        assertEquals("Saarthi's foundation is running.", viewModel.message)
    }
}
