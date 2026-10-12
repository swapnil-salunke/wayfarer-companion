package com.wayfarer.domain

import kotlinx.coroutines.flow.Flow

// GUIDE: Module 3 "Ports" - the domain declares what it needs; data/platform layers implement it.

interface EntryRepository {
    fun observeEntries(tripId: TripId): Flow<List<JournalEntry>>
    suspend fun get(id: EntryId): JournalEntry?
    suspend fun upsert(entry: JournalEntry): Outcome<Unit, DomainError>
}

// GUIDE: Module 9 "Sharing". The domain only knows WHAT to share, never HOW.
data class ShareRequest(
    val text: String,
    val imagePath: String? = null,
    val linkUrl: String? = null,
    val target: ShareTarget = ShareTarget.SystemSheet,
)
enum class ShareTarget { SystemSheet, InstagramStories, SaveToPhotos }
enum class ShareOutcome { Shared, Cancelled, UsedFallbackSheet }

interface ShareService {
    suspend fun share(request: ShareRequest): Outcome<ShareOutcome, DomainError>
}
