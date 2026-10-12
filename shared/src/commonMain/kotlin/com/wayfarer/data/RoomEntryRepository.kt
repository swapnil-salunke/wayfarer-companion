package com.wayfarer.data

import com.wayfarer.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// GUIDE: Module 4 "Repository = adapter". Implements the domain port using Room.
class RoomEntryRepository(private val dao: EntryDao, private val photos: PhotoLocator) : EntryRepository {

    override fun observeEntries(tripId: TripId): Flow<List<JournalEntry>> =
        dao.observe(tripId.value).map { rows -> rows.map { it.toDomain(photos) } }

    override suspend fun get(id: EntryId): JournalEntry? = dao.get(id.value)?.toDomain(photos)

    override suspend fun upsert(entry: JournalEntry): Outcome<Unit, DomainError> = try {
        dao.save(entry.toEntity(), entry.photoPaths.mapIndexed { i, p -> MediaEntity(entry.id.value, photos.toStored(p), i) })
        Outcome.Ok(Unit)
    } catch (t: Throwable) {
        Outcome.Err(DomainError.Storage(t.message ?: "Could not save"))
    }
}

internal fun EntryWithMedia.toDomain(photos: PhotoLocator) = JournalEntry(
    id = EntryId(entry.id), tripId = TripId(entry.tripId), title = entry.title, body = entry.body,
    place = entry.placeName?.let { Place(it, entry.latitude, entry.longitude) },
    photoPaths = media.sortedBy { it.position }.map { photos.resolve(it.path) },
    createdAtMillis = entry.createdAt, updatedAtMillis = entry.updatedAt, deleted = entry.deleted,
)

internal fun JournalEntry.toEntity() = EntryEntity(
    id = id.value, tripId = tripId.value, title = title, body = body,
    placeName = place?.name, latitude = place?.latitude, longitude = place?.longitude,
    createdAt = createdAtMillis, updatedAt = updatedAtMillis, deleted = deleted,
)
