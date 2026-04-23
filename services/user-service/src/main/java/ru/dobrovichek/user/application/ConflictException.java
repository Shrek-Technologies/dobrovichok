package ru.dobrovichek.user.application;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
