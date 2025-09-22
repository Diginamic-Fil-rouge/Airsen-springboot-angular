package fr.airsen.api.external.exception;

/**
 * Exception thrown when INSEE API operations fail.
 * 
 * Handles French administrative and demographic data retrieval errors.
 */
public class InseeApiException extends ExternalApiException {
    
    /**
     * Creates an InseeApiException with a message.
     * 
     * @param message error message
     */
    public InseeApiException(String message) {
        super("INSEE API Error: " + message);
    }
    
    /**
     * Creates an InseeApiException with a message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public InseeApiException(String message, Throwable cause) {
        super("INSEE API Error: " + message, cause);
    }
}