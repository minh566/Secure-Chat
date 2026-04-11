package com.securechat.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.securechat.BuildConfig
import com.securechat.data.local.DatabaseMigrations
import com.securechat.data.local.SecureChatDatabase
import com.securechat.data.local.dao.MessageDao
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
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        if (AuthTestingConfig.shouldDisableAppVerification(BuildConfig.DEBUG)) {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        }
        return auth
    }

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SecureChatDatabase {
        val builder = Room.databaseBuilder(
            context,
            SecureChatDatabase::class.java,
            "securechat.db"
        ).addMigrations(*DatabaseMigrations.ALL)

        if (DatabaseConfigPolicy.shouldUseDestructiveMigration(BuildConfig.DEBUG)) {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    @Provides @Singleton
    fun provideMessageDao(db: SecureChatDatabase): MessageDao = db.messageDao()
}

internal object AuthTestingConfig {
    fun shouldDisableAppVerification(isDebug: Boolean): Boolean = isDebug
}

internal object DatabaseConfigPolicy {
    fun shouldUseDestructiveMigration(isDebug: Boolean): Boolean = isDebug
}
