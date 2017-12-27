package org.opensilk.traveltime.data

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

/**
 * Created by drew on 11/28/17.
 */

@Dao
interface CalendarDAO {
    @Query("SELECT * from event")
    fun getAllEvents(): List<CalendarEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(events: List<@JvmSuppressWildcards CalendarEvent>)

    @Update
    fun update(event: CalendarEvent)

    @Delete
    fun delete(events: List<@JvmSuppressWildcards CalendarEvent>)
}
