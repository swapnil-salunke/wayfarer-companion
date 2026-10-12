package com.wayfarer.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.wayfarer.data.*
import com.wayfarer.domain.*
import com.wayfarer.presentation.*
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.random.Random
import kotlin.time.Clock as KClock
import kotlin.time.ExperimentalTime

// GUIDE: Module 2.4 + Module 10 "Dependency injection with Koin". Common modules here; platform modules in androidMain/iosMain.

fun buildDatabase(builder: RoomDatabase.Builder<WayfarerDatabase>): WayfarerDatabase =
    builder.setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Default).build()

@OptIn(ExperimentalTime::class)
val dataModule = module {
    single<Clock> { Clock { KClock.System.now().toEpochMilliseconds() } }
    single<IdGenerator> { IdGenerator { randomId() } }
    single { get<WayfarerDatabase>().entryDao() }
    single<EntryRepository> { RoomEntryRepository(get(), get()) }
}

val domainModule = module {
    factory { CreateEntryUseCase(get(), get(), get()) }
    factory { UpdateEntryUseCase(get(), get()) }
    factory { ObserveTimelineUseCase(get()) }
    factory { DeleteEntryUseCase(get(), get()) }
    factory { ShareEntryUseCase(get(), get()) }
}

val presentationModule = module {
    viewModel { TimelineViewModel(get(), get()) }
    viewModel { EditorViewModel(get(), get(), get()) }
}

val appModules = listOf(dataModule, domainModule, presentationModule)

// 32 hex chars from the platform-independent RNG - good enough for a local id (a real app: UUID v4/v7).
private fun randomId(): String = buildString { repeat(32) { append("0123456789abcdef"[Random.nextInt(16)]) } }
