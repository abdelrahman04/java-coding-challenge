package com.crewmeister.cmcodingchallenge.exception;

/**
 * Exception thrown when there's an issue communicating with an external API.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}


