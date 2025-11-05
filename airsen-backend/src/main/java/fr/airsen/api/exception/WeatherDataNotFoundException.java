package fr.airsen.api.exception;

public class WeatherDataNotFoundException extends RuntimeException {
    public WeatherDataNotFoundException(String message) {
        super(message);
    }
}

