package com.toan.codraw.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.data.remote.ApiService
import com.toan.codraw.data.remote.BaseUrlInterceptor
import com.toan.codraw.data.remote.DrawingWebSocketListener
import com.toan.codraw.data.remote.WebSocketManager
import com.toan.codraw.data.remote.RetryInterceptor
import com.toan.codraw.data.repository.AuthRepositoryImpl
import com.toan.codraw.data.repository.DrawingRepositoryImpl
import com.toan.codraw.data.repository.ProfileRepositoryImpl
import com.toan.codraw.data.repository.RoomRepositoryImpl
import com.toan.codraw.data.repository.FriendshipRepositoryImpl
import com.toan.codraw.data.repository.ChatRepositoryImpl
import com.toan.codraw.domain.repository.AuthRepository
import com.toan.codraw.domain.repository.DrawingRepository
import com.toan.codraw.domain.repository.ProfileRepository
import com.toan.codraw.domain.repository.RoomRepository
import com.toan.codraw.domain.repository.FriendshipRepository
import com.toan.codraw.domain.repository.ChatRepository
import com.toan.codraw.data.remote.FriendshipApi
import com.toan.codraw.data.remote.ChatApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(baseUrlInterceptor: BaseUrlInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(logging)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SessionManager.DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideFriendshipApi(retrofit: Retrofit): FriendshipApi =
        retrofit.create(FriendshipApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideDrawingWebSocketListener(gson: Gson): DrawingWebSocketListener =
        DrawingWebSocketListener(gson)

    @Provides
    @Singleton
    fun provideWebSocketManager(
        okHttpClient: OkHttpClient,
        listener: DrawingWebSocketListener
    ): WebSocketManager = WebSocketManager(okHttpClient, listener)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDrawingRepository(impl: DrawingRepositoryImpl): DrawingRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(impl: RoomRepositoryImpl): RoomRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindFriendshipRepository(impl: FriendshipRepositoryImpl): FriendshipRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
