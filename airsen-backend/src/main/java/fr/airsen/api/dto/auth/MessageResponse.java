package fr.airsen.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for simple message responses.
 * 
 * This DTO provides a standard format for API responses that only need
 * to convey a simple message to the client, such as success confirmations,
 * error messages, or status updates.
 * 
 * Used for endpoints like:
 * - POST /auth/logout (success message)
 * - Various error responses
 * - Status confirmations
 * 
 * Response Structure:
 * - message: Human-readable message for the user
 * - timestamp: When the response was generated (optional)
 * - success: Whether the operation was successful (optional)
 */
public class MessageResponse {

    /**
     * Human-readable message for the user.
     * 
     * This message can be displayed directly to the user or used
     * for logging and debugging purposes.
     */
    private String message;

    /**
     * Timestamp when the response was generated.
     * 
     * Optional field that can be useful for debugging and logging.
     * Formatted as ISO 8601 datetime string in JSON responses.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;

    /**
     * Whether the operation was successful.
     * 
     * Optional boolean field that can help clients distinguish
     * between success and error messages.
     */
    private Boolean success;

    /**
     * Default constructor for JSON serialization.
     */
    public MessageResponse() {
    }

    /**
     * Constructor with message only.
     * 
     * @param message human-readable message
     */
    public MessageResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with message and success indicator.
     * 
     * @param message human-readable message
     * @param success whether the operation was successful
     */
    public MessageResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with all fields.
     * 
     * @param message human-readable message
     * @param timestamp when the response was generated
     * @param success whether the operation was successful
     */
    public MessageResponse(String message, LocalDateTime timestamp, Boolean success) {
        this.message = message;
        this.timestamp = timestamp;
        this.success = success;
    }

    /**
     * Gets the human-readable message.
     * 
     * @return message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the human-readable message.
     * 
     * @param message message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the response timestamp.
     * 
     * @return timestamp when response was generated
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the response timestamp.
     * 
     * @param timestamp timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets whether the operation was successful.
     * 
     * @return true if successful, false if error, null if not specified
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     * Sets whether the operation was successful.
     * 
     * @param success success indicator to set
     */
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * Checks if this is a success message.
     * 
     * @return true if success is explicitly true
     */
    public boolean isSuccessMessage() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * Checks if this is an error message.
     * 
     * @return true if success is explicitly false
     */
    public boolean isErrorMessage() {
        return Boolean.FALSE.equals(success);
    }

    /**
     * Returns a string representation of this message response.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "MessageResponse{" +
                "message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", success=" + success +
                '}';
    }

    /**
     * Creates a success message response.
     * 
     * @param message success message
     * @return MessageResponse with success=true
     */
    public static MessageResponse success(String message) {
        return new MessageResponse(message, true);
    }

    /**
     * Creates an error message response.
     * 
     * @param message error message
     * @return MessageResponse with success=false
     */
    public static MessageResponse error(String message) {
        return new MessageResponse(message, false);
    }

    /**
     * Creates a simple message response without success indicator.
     * 
     * @param message simple message
     * @return MessageResponse with no success indicator
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }

    /**
     * Creates a builder for constructing MessageResponse instances.
     * 
     * @return new MessageResponse builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing MessageResponse instances.
     */
    public static class Builder {
        private String message;
        private LocalDateTime timestamp;
        private Boolean success;

        /**
         * Sets the message.
         * 
         * @param message human-readable message
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the timestamp.
         * 
         * @param timestamp response timestamp
         * @return this builder
         */
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the timestamp to current time.
         * 
         * @return this builder
         */
        public Builder now() {
            this.timestamp = LocalDateTime.now();
            return this;
        }

        /**
         * Sets the success indicator.
         * 
         * @param success whether operation was successful
         * @return this builder
         */
        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        /**
         * Marks this as a success message.
         * 
         * @return this builder
         */
        public Builder success() {
            this.success = true;
            return this;
        }

        /**
         * Marks this as an error message.
         * 
         * @return this builder
         */
        public Builder error() {
            this.success = false;
            return this;
        }

        /**
         * Builds the MessageResponse instance.
         * 
         * @return configured MessageResponse
         */
        public MessageResponse build() {
            MessageResponse response = new MessageResponse(message, timestamp, success);
            if (timestamp == null) {
                response.setTimestamp(LocalDateTime.now());
            }
            return response;
        }
    }
}