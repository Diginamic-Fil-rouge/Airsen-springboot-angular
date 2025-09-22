package fr.airsen.api.external.exception;

/**
 * Exception thrown when ATMO API operations fail.
 * 
 * Provides specific error handling for air quality data retrieval issues.
 */
public class AtmoApiException extends ExternalApiException {
    
    /**
     * Creates an AtmoApiException with a message.
     * 
     * @param message error message
     */
    public AtmoApiException(String message) {
        super("ATMO API Error: " + message);
    }
    
    /**
     * Creates an AtmoApiException with a message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public AtmoApiException(String message, Throwable cause) {
        super("ATMO API Error: " + message, cause);
    }
}