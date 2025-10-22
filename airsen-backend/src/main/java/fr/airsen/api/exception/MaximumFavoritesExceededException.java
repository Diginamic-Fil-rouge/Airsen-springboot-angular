package fr.airsen.api.exception;

/**
 * Exception thrown when a user attempts to add more than 10 favorites.
 *
 * Business Rule: Each user is limited to a maximum of 10 favorite communes
 * to ensure reasonable resource usage and maintain performance.
 *
 * HTTP Status: 400 Bad Request
 */
public class MaximumFavoritesExceededException extends RuntimeException {

    /**
     * Constructs a new MaximumFavoritesExceededException with the specified detail message.
     *
     * @param message the detail message explaining why the limit was exceeded
     */
    public MaximumFavoritesExceededException(String message) {
        super(message);
    }

    /**
     * Constructs a new MaximumFavoritesExceededException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public MaximumFavoritesExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
