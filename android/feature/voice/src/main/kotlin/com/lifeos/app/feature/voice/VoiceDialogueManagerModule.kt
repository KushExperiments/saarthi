package com.lifeos.app.feature.voice

import com.lifeos.app.core.interaction.DialogueManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceDialogueManagerModule {
    @Binds
    @Singleton
    abstract fun bindDialogueManager(impl: VoiceDialogueManager): DialogueManager
}
