package com.example.iot.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.iot.data.local.AppDatabase
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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "iot.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideRemoteDao(db: AppDatabase): RemoteDao = db.remoteDao()
}
