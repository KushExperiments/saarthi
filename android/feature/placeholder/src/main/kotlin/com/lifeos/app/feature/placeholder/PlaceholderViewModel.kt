package com.lifeos.app.feature.placeholder

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaceholderViewModel @Inject constructor(
    private val greetingProvider: GreetingProvider,
) : ViewModel() {
    val message: String = greetingProvider.greet()
}
