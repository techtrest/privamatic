package com.techtrest.privamatic.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.techtrest.privamatic.data.model.PrivacySnapshot

@Dao
interface PrivacySnapshotDao {
    @Insert
    suspend fun insert(snapshot: PrivacySnapshot)

    @Query("SELECT * FROM privacy_snapshots WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSnapshotsSince(since: Long): List<PrivacySnapshot>

    @Query("SELECT * FROM privacy_snapshots ORDER BY timestamp ASC")
    suspend fun getAllSnapshots(): List<PrivacySnapshot>

    @Update
    suspend fun update(snapshot: PrivacySnapshot)

    /** The day's newest point; days recorded before every scan set the point have at most one. */
    @Query(
        "SELECT * FROM privacy_snapshots WHERE timestamp >= :dayStart AND timestamp < :dayEnd " +
            "ORDER BY timestamp DESC, id DESC LIMIT 1"
    )
    suspend fun latestSnapshotForDay(dayStart: Long, dayEnd: Long): PrivacySnapshot?

    /**
     * Sets the day's point: inserts [point] if the day [dayStart, dayEnd) has none, otherwise
     * overwrites the day's newest row with it (same row id). One transaction, so two scans that
     * finish together (app scan, then the widget scan it triggers) can't both insert.
     */
    @Transaction
    suspend fun setPointForDay(dayStart: Long, dayEnd: Long, point: PrivacySnapshot) {
        val existing = latestSnapshotForDay(dayStart, dayEnd)
        if (existing == null) insert(point) else update(point.copy(id = existing.id))
    }

    @Query("DELETE FROM privacy_snapshots")
    suspend fun deleteAll()

    @Query("SELECT MIN(timestamp) FROM privacy_snapshots")
    suspend fun getEarliestTimestamp(): Long?
}
