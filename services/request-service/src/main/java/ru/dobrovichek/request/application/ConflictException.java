package ru.dobrovichek.request.application;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
