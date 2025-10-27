package fr.airsen.api.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for.
 *
 * Security Rule: Users can only manage their own favorites.
 * Administrators can manage any user's favorites.
 *
 * HTTP Status: 403 Forbidden
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Constructs a new ForbiddenException with the specified detail message.
     *
     * @param message the detail message explaining the forbidden access
     */
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * Constructs a new ForbiddenException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
