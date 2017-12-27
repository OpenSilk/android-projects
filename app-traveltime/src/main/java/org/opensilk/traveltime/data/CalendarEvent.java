package org.opensilk.traveltime.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.api.services.calendar.model.Event;

/**
 * Created by drew on 11/28/17.
 */
@Entity(tableName = "event")
public class CalendarEvent {
    @PrimaryKey @NonNull private String id;
    private String summary;
    private String description;
    private long start;
    private long end;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public static CalendarEvent from(Event event) {
        CalendarEvent e = new CalendarEvent();
        e.setId(event.getId());
        e.setSummary(event.getSummary());
        e.setDescription(event.getDescription());
        //TODO getDateTime is null for all day events
        e.setStart(event.getStart().getDateTime().getValue());
        e.setEnd(event.getEnd().getDateTime().getValue());
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalendarEvent that = (CalendarEvent) o;

        if (start != that.start) return false;
        if (end != that.end) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (summary != null ? !summary.equals(that.summary) : that.summary != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (summary != null ? summary.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
