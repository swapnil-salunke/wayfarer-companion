package com.wayfarer.presentation

import androidx.compose.runtime.mutableStateListOf
import com.wayfarer.domain.JournalEntryId
import com.wayfarer.domain.TripId

// GUIDE: Module 7 "Navigation". Route = every screen as a type. The sprint uses a plain back stack;
// Module 7 shows swapping the `when` in WayfarerApp for Navigation 3's NavDisplay (same Route types, make them NavKey).
sealed interface Route {
    data object Timeline : Route
    data class Editor(val entryId: JournalEntryId? = null) : Route
    data class SharePreview(val entryId: JournalEntryId) : Route
}

val DefaultTripId = TripId("default-trip") // Trip list is cut from the sprint; one trip is enough to learn the architecture.

class Navigator {
    val backStack = mutableStateListOf<Route>(Route.Timeline)
    fun go(route: Route) { backStack.add(route) }
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
}
