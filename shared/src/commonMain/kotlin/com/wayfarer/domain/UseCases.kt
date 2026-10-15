package com.wayfarer.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// GUIDE: Module 3 "Use cases" - one business action each; ViewModels call these, never the DAO.

class CreateEntryUseCase(
    private val repository: EntryRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(
        tripId: TripId, title: String, body: String, placeName: String?, photoPaths: List<String>,
    ): Outcome<JournalEntry, DomainError> {
        if (title.isBlank() && body.isBlank() && photoPaths.isEmpty())
            return Outcome.Err(DomainError.Validation("Write something or add a photo first."))
        val now = clock.nowMillis()
        val entry = JournalEntry(
            id = JournalEntryId(ids.newId()), tripId = tripId,
            title = title.trim(), body = body.trim(),
            place = placeName?.takeIf { it.isNotBlank() }?.let { Place(it.trim()) },
            photoPaths = photoPaths, createdAtMillis = now, updatedAtMillis = now,
        )
        return when (val r = repository.upsert(entry)) {
            is Outcome.Ok -> Outcome.Ok(entry)
            is Outcome.Err -> r
        }
    }
}

class UpdateEntryUseCase(private val repository: EntryRepository, private val clock: Clock) {
    suspend operator fun invoke(
        id: JournalEntryId, title: String, body: String, placeName: String?, photoPaths: List<String>,
    ): Outcome<JournalEntry, DomainError> {
        if (title.isBlank() && body.isBlank() && photoPaths.isEmpty())
            return Outcome.Err(DomainError.Validation("Write something or add a photo first."))
        val old = repository.get(id) ?: return Outcome.Err(DomainError.NotFound)
        val updated = old.copy(
            title = title.trim(), body = body.trim(),
            place = placeName?.takeIf { it.isNotBlank() }?.let { Place(it.trim()) },
            photoPaths = photoPaths, updatedAtMillis = clock.nowMillis(),
        )
        return when (val r = repository.upsert(updated)) {
            is Outcome.Ok -> Outcome.Ok(updated)
            is Outcome.Err -> r
        }
    }
}

class ObserveTimelineUseCase(private val repository: EntryRepository) {
    /** Newest first, tombstoned (deleted) entries hidden. */
    operator fun invoke(tripId: TripId): Flow<List<JournalEntry>> =
        repository.observeEntries(tripId).map { list ->
            list.filterNot { it.deleted }.sortedByDescending { it.createdAtMillis }
        }
}

class DeleteEntryUseCase(private val repository: EntryRepository, private val clock: Clock) {
    /** Soft delete = tombstone, so a future sync can tell the server. Guide: Module 11. */
    suspend operator fun invoke(id: JournalEntryId): Outcome<Unit, DomainError> {
        val e = repository.get(id) ?: return Outcome.Err(DomainError.NotFound)
        return repository.upsert(e.copy(deleted = true, updatedAtMillis = clock.nowMillis()))
    }
}

class ShareEntryUseCase(private val repository: EntryRepository, private val share: ShareService) {
    suspend operator fun invoke(id: JournalEntryId, imagePath: String?, target: ShareTarget): Outcome<ShareOutcome, DomainError> {
        val e = repository.get(id) ?: return Outcome.Err(DomainError.NotFound)
        val text = buildString {
            if (e.title.isNotBlank()) appendLine(e.title)
            e.place?.let { appendLine("📍 ${it.name}") }
            if (e.body.isNotBlank()) append(e.body)
        }.trim()
        return share.share(ShareRequest(text = text, imagePath = imagePath ?: e.photoPaths.firstOrNull(), target = target))
    }
}
