package com.wayfarer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// GUIDE: Module 4 "Data layer / Room". Entities are DB shapes; mappers convert to domain types.

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String,
    val body: String,
    val placeName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
)

@Entity(tableName = "media", primaryKeys = ["entryId", "path"])
data class MediaEntity(val entryId: String, val path: String, val position: Int)

data class EntryWithMedia(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId") val media: List<MediaEntity>,
)

@Dao
interface EntryDao {
    @Transaction
    @Query("SELECT * FROM entries WHERE tripId = :tripId")
    fun observe(tripId: String): Flow<List<EntryWithMedia>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: String): EntryWithMedia?

    @Upsert suspend fun upsertEntry(e: EntryEntity)
    @Query("DELETE FROM media WHERE entryId = :entryId") suspend fun clearMedia(entryId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMedia(items: List<MediaEntity>)

    @Transaction
    suspend fun save(e: EntryEntity, media: List<MediaEntity>) {
        upsertEntry(e); clearMedia(e.id); insertMedia(media)
    }
}

@Database(entities = [EntryEntity::class, MediaEntity::class], version = 1)
@ConstructedBy(WayfarerDatabaseConstructor::class)
abstract class WayfarerDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}

// Room's compiler generates the 'actual' for each platform.
@Suppress("KotlinNoActualForExpect")
expect object WayfarerDatabaseConstructor : RoomDatabaseConstructor<WayfarerDatabase> {
    override fun initialize(): WayfarerDatabase
}
