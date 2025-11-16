package com.example.iot.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.iot.data.local.AppDatabase
import com.example.iot.data.local.LearnedCommandDao
import com.example.iot.data.local.RemoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE remote_profiles ADD COLUMN deviceType TEXT NOT NULL DEFAULT 'AC'"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learned_commands (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId INTEGER NOT NULL,
                deviceType TEXT NOT NULL,
                key TEXT NOT NULL,
                protocol TEXT NOT NULL,
                code TEXT NOT NULL,
                bits INTEGER NOT NULL,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_learned_commands_remoteId_key ON learned_commands(remoteId, key)"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "iot.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideRemoteDao(db: AppDatabase): RemoteDao = db.remoteDao()

    @Provides
    fun provideLearnedCommandDao(db: AppDatabase): LearnedCommandDao = db.learnedCommandDao()
}