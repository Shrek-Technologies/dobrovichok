package ru.dobrovichek.events;

public final class RequestEventTopology {

    public static final String REQUEST_EVENTS_EXCHANGE = "request.events";
    public static final String REQUEST_CREATED_ROUTING_KEY = "request.created";
    public static final String REQUEST_STATUS_CHANGED_ROUTING_KEY = "request.status-changed";
    public static final String REQUEST_VOLUNTEER_ABANDONED_ROUTING_KEY = "request.volunteer-abandoned";

    public static final String REQUEST_CREATED_QUEUE = "notification.request-created";
    public static final String REQUEST_STATUS_CHANGED_QUEUE = "notification.request-status-changed";
    public static final String NOTIFICATION_REQUEST_VOLUNTEER_ABANDONED_QUEUE = "notification.request-volunteer-abandoned";
    public static final String USER_REQUEST_STATUS_CHANGED_QUEUE = "user.request-status-changed";
    public static final String USER_REQUEST_VOLUNTEER_ABANDONED_QUEUE = "user.request-volunteer-abandoned";

    private RequestEventTopology() {
    }
}
