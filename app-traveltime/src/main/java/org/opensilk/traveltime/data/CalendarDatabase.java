package org.opensilk.traveltime.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by drew on 11/28/17.
 */
@Database(
        entities = {
                CalendarEvent.class
        },
        version = 1
)
public abstract class CalendarDatabase extends RoomDatabase {
    public abstract CalendarDAO calendarDAO();
}
