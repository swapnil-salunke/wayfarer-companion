package com.wayfarer

import com.wayfarer.domain.*
import kotlinx.coroutines.flow.*

// GUIDE: Module 12 "Testing": a fake = a tiny in-memory stand-in, no mocking library needed.
class FakeEntryRepository : EntryRepository {
    private val store = MutableStateFlow<Map<String, JournalEntry>>(emptyMap())
    override fun observeEntries(tripId: TripId) = store.map { m -> m.values.filter { it.tripId == tripId } }
    override suspend fun get(id: JournalEntryId) = store.value[id.value]
    override suspend fun upsert(entry: JournalEntry): Outcome<Unit, DomainError> {
        store.update { it + (entry.id.value to entry) }; return Outcome.Ok(Unit)
    }
}
