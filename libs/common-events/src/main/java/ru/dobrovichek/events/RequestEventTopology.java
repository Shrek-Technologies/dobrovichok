package ru.dobrovichek.events;

public final class RequestEventTopology {

    public static final String REQUEST_EVENTS_EXCHANGE = "request.events";
    public static final String REQUEST_CREATED_ROUTING_KEY = "request.created";
    public static final String REQUEST_STATUS_CHANGED_ROUTING_KEY = "request.status-changed";

    public static final String REQUEST_CREATED_QUEUE = "notification.request-created";
    public static final String REQUEST_STATUS_CHANGED_QUEUE = "notification.request-status-changed";

    private RequestEventTopology() {
    }
}
