package fr.airsen.api.external.exception;

/**
 * Base exception for external API integration errors.
 * 
 * Provides a common base for all external API related exceptions
 * to enable centralized error handling and monitoring.
 */
public class ExternalApiException extends RuntimeException {

    /**
     * Creates an ExternalApiException with a message.
     * 
     * @param message error message
     */
    public ExternalApiException(String message) {
        super(message);
    }

    /**
     * Creates an ExternalApiException with a message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}