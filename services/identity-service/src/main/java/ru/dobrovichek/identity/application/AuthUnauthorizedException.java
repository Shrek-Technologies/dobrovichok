package ru.dobrovichek.identity.application;

public class AuthUnauthorizedException extends RuntimeException {
    public AuthUnauthorizedException(String message) {
        super(message);
    }
}
