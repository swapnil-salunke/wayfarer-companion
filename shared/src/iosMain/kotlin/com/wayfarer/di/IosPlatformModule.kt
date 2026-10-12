package com.wayfarer.di

import androidx.compose.ui.window.ComposeUIViewController
import androidx.room.Room
import com.wayfarer.WayfarerApp
import com.wayfarer.data.WayfarerDatabase
import com.wayfarer.domain.ShareService
import com.wayfarer.platform.*
import org.koin.dsl.module
import platform.Foundation.*
import platform.UIKit.UIViewController

// GUIDE: Module 2.4 / 10.3. Swift calls MainViewController() - see iosApp/iosApp/ContentView.swift.
fun iosPlatformModule(facebookAppId: String = "YOUR_FACEBOOK_APP_ID") = module {
    single { ShareConfig(facebookAppId) }
    single<ShareService> { IosShareService(get()) }
    single<FileStore> { IosFileStore() }
    single {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        buildDatabase(Room.databaseBuilder<WayfarerDatabase>(name = "$docs/wayfarer.db"))
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController { WayfarerApp(iosPlatformModule()) }
