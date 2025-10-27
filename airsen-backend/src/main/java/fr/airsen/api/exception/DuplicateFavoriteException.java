package fr.airsen.api.exception;

/**
 * Exception thrown when a user attempts to favorite a commune that is already in their favorites.
 *
 * Business Rule: Each user-commune favorite relationship must be unique.
 * The composite key (userId, communeId) prevents duplicates at database level,
 * but this exception provides a more user-friendly error message.
 *
 * HTTP Status: 409 Conflict
 */
public class DuplicateFavoriteException extends RuntimeException {

    /**
     * Constructs a new DuplicateFavoriteException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate favorite
     */
    public DuplicateFavoriteException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateFavoriteException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DuplicateFavoriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
