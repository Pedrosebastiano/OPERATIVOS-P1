package modelos;

import java.util.ArrayList;
import java.util.List;

public class EventLogger {
    private List<String> events;
    private static final int MAX_EVENTS = 1000;

    public EventLogger() {
        this.events = new ArrayList<>();
    }

    public synchronized void logEvent(String event) {
        String timestamp = java.time.LocalTime.now().toString();
        String logEntry = "[" + timestamp + "] " + event;
        events.add(logEntry);
        
        if (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
        
        System.out.println(logEntry);
    }

    public synchronized List<String> getEvents() {
        return new ArrayList<>(events);
    }

    public synchronized String getEventsAsString() {
        StringBuilder sb = new StringBuilder();
        for (String event : events) {
            sb.append(event).append("\n");
        }
        return sb.toString();
    }

    public synchronized void clearEvents() {
        events.clear();
    }
}