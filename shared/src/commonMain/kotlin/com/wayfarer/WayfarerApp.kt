package com.wayfarer

import androidx.compose.runtime.*
import com.wayfarer.presentation.*
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import com.wayfarer.di.appModules

// GUIDE: Module 2.4 "One entry point shared by both platforms". Each platform passes in its own platformModule.
@Composable
fun WayfarerApp(platformModule: Module) {
    KoinApplication(application = { modules(platformModule, *appModules.toTypedArray()) }) {
        WayfarerTheme {
            val navigator = remember { Navigator() }
            when (val route = navigator.backStack.last()) {
                Route.Timeline -> {
                    val vm = koinViewModel<TimelineViewModel>()
                    val state by vm.state.collectAsState()
                    TimelineScreen(state, onAdd = { navigator.go(Route.Editor()) },
                        onOpenShare = { navigator.go(Route.SharePreview(it)) },
                        onEdit = { navigator.go(Route.Editor(it)) }, onDelete = vm::onDelete)
                }
                is Route.Editor -> {
                    val vm = koinViewModel<EditorViewModel>()
                    val state by vm.state.collectAsState()
                    LaunchedEffect(route) { vm.start(route.entryId) }
                    LaunchedEffect(vm) { vm.effects.collect { if (it is EditorEffect.Saved) navigator.back() } }
                    EditorScreen(state, route.entryId != null, vm::onEvent, navigator::back)
                }
                is Route.SharePreview -> SharePreviewScreen(route.entryId, navigator::back)
            }
        }
    }
}
