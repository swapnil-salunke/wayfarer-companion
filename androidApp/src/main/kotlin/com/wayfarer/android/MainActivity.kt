package com.wayfarer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wayfarer.WayfarerApp
import com.wayfarer.di.androidPlatformModule
import com.wayfarer.platform.CurrentActivityProvider

// GUIDE: Module 2.4. The Android side is ~10 lines: build the platform module, hand it to the shared app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activities = CurrentActivityProvider(application).also { it.seed(this) }
        setContent { WayfarerApp(androidPlatformModule(applicationContext, facebookAppId = "YOUR_FACEBOOK_APP_ID", activities = activities)) }
    }
}
