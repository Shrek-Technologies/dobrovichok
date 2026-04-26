package ru.dobrovichek.request.exception;

import java.util.UUID;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(UUID requestId) {
        super("Request not found: " + requestId);
    }
}
