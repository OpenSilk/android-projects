package org.opensilk.traveltime.api;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Setting;

import org.opensilk.traveltime.data.AuthHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

/**
 * Wraps/hides the calendar api, allowing easier mocking for tests
 *
 * Created by drew on 11/12/17.
 */
public class CalendarApi {

    private static final long EVENT_TIMESPAN = 1000000000; //~11 days

    private final AuthHelper authHelper;

    @Inject
    public CalendarApi(
            AuthHelper authHelper
    ) {
        this.authHelper = authHelper;
    }

    public boolean poke() throws IOException {
        Calendar.Settings api = authHelper.calendar().settings();
        Calendar.Settings.Get get = api.get("dateFieldOrder");
        Setting setting = get.execute();
        return setting.getValue() != null;
    }

    public List<Event> getAllEvents() throws IOException {
        Calendar.Events api = authHelper.calendarEvents();
        List<Event> eventsList = new ArrayList<>();
        String pageToken = null;
        DateTime startTime = new DateTime(new Date());
        DateTime endTime = new DateTime(startTime.getValue() + EVENT_TIMESPAN,
                startTime.getTimeZoneShift());
        do {
            Events events = api.list(AuthHelper.CALENDAR_ID)
                    .setTimeMin(startTime)
                    .setTimeMax(endTime)
                    .setPageToken(pageToken).execute();
            eventsList.addAll(events.getItems());
            pageToken = events.getNextPageToken();
        } while (pageToken != null);
        return eventsList;
    }

    public void deleteEvents(List<Event> events) throws IOException {
        Calendar.Events api = authHelper.calendarEvents();
        for (Event e: events) {
            api.delete(AuthHelper.CALENDAR_ID, e.getId()).execute();
        }
    }

    public void deleteEvent(Event event) throws IOException {
        Calendar.Events api = authHelper.calendarEvents();
        api.delete(AuthHelper.CALENDAR_ID, event.getId()).execute();
    }

    public void createEvents(List<Event> events) throws IOException {
        Calendar.Events api = authHelper.calendarEvents();
        for (Event e: events) {
            api.insert(AuthHelper.CALENDAR_ID, e).execute();
        }
    }

    public Event createEvent(Event event) throws IOException {
        Calendar.Events api = authHelper.calendarEvents();
        return api.insert(AuthHelper.CALENDAR_ID, event).execute();
    }


}
