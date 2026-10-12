package com.wayfarer

import com.wayfarer.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UseCaseTests {
    private val trip = TripId("t")
    private var n = 0
    private val repo = FakeEntryRepository()
    private val create = CreateEntryUseCase(repo, { 1_000L + n }, { "id${n++}" })

    @Test fun emptyEntryIsRejected() = runTest {
        val r = create(trip, "  ", "", null, emptyList())
        assertIs<Outcome.Err<DomainError>>(r)
        assertIs<DomainError.Validation>(r.error)
    }

    @Test fun createdEntryAppearsNewestFirst() = runTest {
        create(trip, "first", "", null, emptyList())
        create(trip, "second", "", "Lisbon", emptyList())
        val list = ObserveTimelineUseCase(repo)(trip).first()
        assertEquals(listOf("second", "first"), list.map { it.title })
        assertEquals("Lisbon", list.first().place?.name)
    }

    @Test fun deleteIsATombstoneNotARemoval() = runTest {
        val e = (create(trip, "x", "", null, emptyList()) as Outcome.Ok).value
        DeleteEntryUseCase(repo) { 9_999L }(e.id)
        assertTrue(repo.get(e.id)!!.deleted)                       // still stored (sync can see it)
        assertTrue(ObserveTimelineUseCase(repo)(trip).first().isEmpty()) // but hidden from the timeline
    }
}
