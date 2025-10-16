package fr.airsen.api.external.exception;

/**
 * Exception thrown when weather API operations fail.
 * 
 * Handles weather data retrieval errors with appropriate context.
 */
public class WeatherApiException extends ExternalApiException {
    
    /**
     * Creates a WeatherApiException with a message.
     * 
     * @param message error message
     */
    public WeatherApiException(String message) {
        super("Weather API Error: " + message);
    }
    
    /**
     * Creates a WeatherApiException with a message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public WeatherApiException(String message, Throwable cause) {
        super("Weather API Error: " + message, cause);
    }
}