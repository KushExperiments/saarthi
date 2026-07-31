package com.lifeos.app.core.ai

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindAiProvider(impl: GroqAiProvider): AiProvider

    @Binds
    @Singleton
    abstract fun bindAiApiKeyStore(impl: EncryptedAiApiKeyStore): AiApiKeyStore

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

        @Provides
        @Singleton
        fun provideGroqEndpoints(): GroqEndpoints = GroqEndpoints()
    }
}
