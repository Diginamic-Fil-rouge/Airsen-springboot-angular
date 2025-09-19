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

    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;

    private Boolean success;

    public MessageResponse() {
    }

    public MessageResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public MessageResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public MessageResponse(String message, LocalDateTime timestamp, Boolean success) {
        this.message = message;
        this.timestamp = timestamp;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getSuccess() {
        return success;
    }

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

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder now() {
            this.timestamp = LocalDateTime.now();
            return this;
        }

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder success() {
            this.success = true;
            return this;
        }

        public Builder error() {
            this.success = false;
            return this;
        }

        public MessageResponse build() {
            MessageResponse response = new MessageResponse(message, timestamp, success);
            if (timestamp == null) {
                response.setTimestamp(LocalDateTime.now());
            }
            return response;
        }
    }
}