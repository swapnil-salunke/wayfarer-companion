package com.wayfarer.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.wayfarer.data.PhotoLocator
import com.wayfarer.data.WayfarerDatabase
import java.io.File
import com.wayfarer.domain.ShareService
import com.wayfarer.platform.*
import org.koin.dsl.module

// GUIDE: Module 10.3 "Platform modules". Everything that needs an Android Context is built here.
fun androidPlatformModule(
    context: Context, facebookAppId: String,
    activities: CurrentActivityProvider = CurrentActivityProvider(context.applicationContext as Application),
) = module {
    single { ShareConfig(facebookAppId) }
    single { activities }
    single<ShareService> { AndroidShareService(context, get(), get()) }
    single<FileStore> { AndroidFileStore(context) }
    single { PhotoLocator(File(context.filesDir, "photos").absolutePath) }
    single {
        buildDatabase(Room.databaseBuilder<WayfarerDatabase>(context, context.getDatabasePath("wayfarer.db").absolutePath))
    }
}
