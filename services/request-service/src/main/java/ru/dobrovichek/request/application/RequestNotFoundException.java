package ru.dobrovichek.request.application;

import java.util.UUID;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(UUID requestId) {
        super("Request not found: " + requestId);
    }
}
