package com.securechat.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.securechat.data.local.SecureChatDatabase
import com.securechat.data.local.dao.MessageDao
import com.securechat.data.repository.AuthRepositoryImpl
import com.securechat.data.repository.CallRepositoryImpl
import com.securechat.data.repository.ChatRepositoryImpl
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.CallRepository
import com.securechat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SecureChatDatabase =
        Room.databaseBuilder(
            context,
            SecureChatDatabase::class.java,
            "securechat.db"
        ).fallbackToDestructiveMigration().build()

    @Provides @Singleton
    fun provideMessageDao(db: SecureChatDatabase): MessageDao = db.messageDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds @Singleton
    abstract fun bindCallRepository(impl: CallRepositoryImpl): CallRepository
}
