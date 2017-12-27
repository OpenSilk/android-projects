package org.opensilk.traveltime.data;

import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

/**
 * Performs the calendar sync, manages removing/adding/calculating travel times for events
 *
 * Created by drew on 11/12/17.
 */

public class CalendarSync {

    private static final String TAG = "CalendarSync";

    private final CalendarApi calendarApi;
    private final MapsApi mapsApi;
    private final CalendarDAO calendarDAO;

    @Inject
    public CalendarSync(
            CalendarApi calendarApi,
            MapsApi mapsApi,
            CalendarDAO calendarDAO
    ) {
        this.calendarApi = calendarApi;
        this.mapsApi = mapsApi;
        this.calendarDAO = calendarDAO;
    }

    public void syncCalendar() {
        try {
            List<Event> items = calendarApi.getAllEvents();
            List<Event> travelEvents = new ArrayList<>();
            List<Event> normalEvents = new ArrayList<>();
            List<CalendarEvent> pulledEvents = new ArrayList<>();
            for (Event event : items) {
                //TODO doesnt work extendedProperies always null
                /*
                Event.ExtendedProperties extendedProperties = event.getExtendedProperties();
                if (extendedProperties != null) {
                    if (extendedProperties.get("travel") == Boolean.TRUE) {
                        travelEvents.add(event);
                        continue;
                    }
                }*/
                if (event.getSummary() != null && event.getSummary().startsWith("Travel")) {
                    travelEvents.add(event);
                } else {
                    normalEvents.add(event);
                    pulledEvents.add(CalendarEvent.from(event));
                }
            }
            List<CalendarEvent> storedEvents = calendarDAO.getAllEvents();

            if (pulledEvents.size() == storedEvents.size()) {
                boolean updated = false;
                for (CalendarEvent pulledEvent : pulledEvents) {
                    Iterator<CalendarEvent> storedII = storedEvents.iterator();
                    while (storedII.hasNext()) {
                        CalendarEvent storedEvent = storedII.next();
                        if (pulledEvent.equals(storedEvent)) {
                            //nothing changed
                            storedII.remove();
                        } else if (pulledEvent.getId().equals(storedEvent.getId())) {
                            //something changed
                            updated = true;
                            calendarDAO.update(pulledEvent);
                            storedII.remove();
                        }
                    }
                }

                if (!updated && storedEvents.isEmpty()) {
                    Log.i(TAG, "No change since last sync.");
                    return;
                }
            }

            //cleanup then add the new ones
            calendarDAO.delete(storedEvents);
            calendarDAO.insert(pulledEvents);

            Log.i(TAG, String.format("Removing %d travel events", travelEvents.size()));
            calendarApi.deleteEvents(travelEvents);

            if (normalEvents.isEmpty()) {
                Log.i(TAG, "No events to process");
                return;
            }

            //sort the events by start time
            Collections.sort(normalEvents, new Comparator<Event>() {
                        @Override
                        public int compare(Event o1, Event o2) {
                            return (int) (o1.getStart().getDateTime().getValue()
                                    - o2.getStart().getDateTime().getValue());
                        }
                    });

            Log.i(TAG, String.format("Begin calendar sync: %d events to process", normalEvents.size()));
            Event previousEvent = null;
            for (Event event: normalEvents) {

                String location = event.getLocation();
                if (TextUtils.isEmpty(location)) {
                    Log.i(TAG, "Skipping event without location " + event.getSummary());
                    previousEvent = null;
                    continue;
                }

                DateTime eventStart = event.getStart().getDateTime();
                if (eventStart == null) {
                    Log.i(TAG, "Skipping all day event " + event.getSummary());
                    previousEvent = null;
                    continue;
                }

                String startAddr = "";
                long traveltime = 0;
                if (previousEvent != null && (eventStart.getValue() -
                        previousEvent.getEnd().getDateTime().getValue()) < 3600000) {
                    //if within an hour use previous event
                    traveltime = mapsApi.getTravelTimeMilli(previousEvent.getLocation(), location);
                    startAddr = previousEvent.getLocation();
                } else if (isWithinWorkHours(eventStart)) {
                    //use job address
                    //TODO
                } else {
                    //use home address
                    traveltime = mapsApi.getTravelTimeFromHome(location);
                    startAddr = "Home";
                }

                if (traveltime <= 0) {
                    previousEvent = null;
                    Log.i(TAG, "Unable to find travel time for " + event.getSummary());
                    continue;
                }

                createTravelEvent(event, traveltime, startAddr);

                previousEvent = event;
            }
            Log.i(TAG, "Finished processing events");
        } catch (Exception e) {
            Log.e("fuck", "getting events failed", e);
        }
    }

    private void createTravelEvent(Event event, long travelTimeMilli, String startAddr) throws IOException {
        // Prepare extended property for travel event.
        Event.ExtendedProperties ep = new Event.ExtendedProperties();
        ep.set("travel", true);

        DateTime eventStart = event.getStart().getDateTime();
        EventDateTime travelStart = new EventDateTime().setDateTime(
                new DateTime(eventStart.getValue() - travelTimeMilli, eventStart.getTimeZoneShift()));
        EventDateTime travelEnd = new EventDateTime().setDateTime(eventStart);

        Event travelEvent = new Event()
                .setSummary("Travel to " + event.getSummary())
                .setDescription(String.format("Travel from '%s' to '%s'", startAddr, event.getLocation()))
                .setExtendedProperties(ep)
                .setColorId("10")
                .setStart(travelStart)
                .setEnd(travelEnd)
                ;

        Event createdEvent = calendarApi.createEvent(travelEvent);
        Log.i(TAG, String.format("Created travel event: summary=%s id=%s",
                createdEvent.getSummary(), createdEvent.getId()));

    }

    private boolean isWithinWorkHours(DateTime start) {
        return false;
    }

}
