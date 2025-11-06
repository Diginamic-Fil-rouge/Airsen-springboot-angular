package fr.airsen.api.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when weather data is updated in the database.
 *
 * This event triggers cache eviction listeners to refresh cached weather data
 * after scheduled updates or manual data refresh operations.
 */
public class WeatherDataUpdatedEvent extends ApplicationEvent {

    private final int communesUpdated;
    private final String updateSource;

    /**
     * Create a new WeatherDataUpdatedEvent.
     *
     * @param source the object that published the event
     * @param communesUpdated number of communes that had weather data updated
     * @param updateSource source of the update (e.g., "SCHEDULED_DAILY", "MANUAL_REFRESH")
     */
    public WeatherDataUpdatedEvent(Object source, int communesUpdated, String updateSource) {
        super(source);
        this.communesUpdated = communesUpdated;
        this.updateSource = updateSource;
    }

    public int getCommunesUpdated() {
        return communesUpdated;
    }

    public String getUpdateSource() {
        return updateSource;
    }

    @Override
    public String toString() {
        return "WeatherDataUpdatedEvent{" +
                "communesUpdated=" + communesUpdated +
                ", updateSource='" + updateSource + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
