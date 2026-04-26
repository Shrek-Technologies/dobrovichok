package ru.dobrovichek.user.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.dobrovichek.events.RequestEventTopology;
import ru.dobrovichek.events.VolunteerAbandonedRequestEvent;
import ru.dobrovichek.user.application.VolunteerRatingService;

@Component
@ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class VolunteerAbandonedEventListener {

    private final VolunteerRatingService volunteerRatingService;

    public VolunteerAbandonedEventListener(VolunteerRatingService volunteerRatingService) {
        this.volunteerRatingService = volunteerRatingService;
    }

    @RabbitListener(queues = RequestEventTopology.USER_REQUEST_VOLUNTEER_ABANDONED_QUEUE)
    public void handle(VolunteerAbandonedRequestEvent event) {
        volunteerRatingService.applyAbandonmentPenalty(
                event.volunteerId(),
                event.requestId(),
                event.wardId()
        );
    }
}
