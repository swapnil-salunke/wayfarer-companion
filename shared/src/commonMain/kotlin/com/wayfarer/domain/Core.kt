package com.wayfarer.domain

import kotlin.jvm.JvmInline

// GUIDE: Module 3 "The domain layer" - pure Kotlin, no Android/iOS/Room imports allowed here.

/** Success-or-failure without exceptions. Guide Module 3, "Outcome". */
sealed interface Outcome<out T, out E> {
    data class Ok<T>(val value: T) : Outcome<T, Nothing>
    data class Err<E>(val error: E) : Outcome<Nothing, E>
}

sealed interface DomainError {
    data class Validation(val message: String) : DomainError
    data object NotFound : DomainError
    data class Storage(val message: String) : DomainError
    data class Platform(val message: String) : DomainError
}

/** Value classes stop you passing a TripId where a JournalEntryId is expected. */
@JvmInline value class TripId(val value: String)
@JvmInline value class JournalEntryId(val value: String)

data class Place(val name: String, val latitude: Double? = null, val longitude: Double? = null)

/** Offline-first columns (id from phone, updatedAt, deleted) = easy sync later. Guide: Sprint block 3 cut line. */
data class JournalEntry(
    val id: JournalEntryId,
    val tripId: TripId,
    val title: String,
    val body: String,
    val place: Place?,
    val photoPaths: List<String>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deleted: Boolean = false,
)

data class Trip(val id: TripId, val name: String)

/** Things the domain needs from the outside world - tiny interfaces so tests can fake them. */
fun interface Clock { fun nowMillis(): Long }
fun interface IdGenerator { fun newId(): String }
