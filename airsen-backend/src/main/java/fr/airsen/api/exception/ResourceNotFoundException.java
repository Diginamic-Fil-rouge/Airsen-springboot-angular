package fr.airsen.api.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 * 
 * This is typically used for entity lookups that fail to find
 * matching records in the database.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}