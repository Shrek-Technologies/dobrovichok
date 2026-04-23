package ru.dobrovichek.request.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.dobrovichek.events.RequestEventTopology;
import ru.dobrovichek.request.application.port.out.RequestEventPublisher;

@Configuration
public class RabbitRequestEventConfiguration {

    @Bean
    @ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
    public TopicExchange requestEventsExchange() {
        return new TopicExchange(RequestEventTopology.REQUEST_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "dobrovichek.messaging.enabled", havingValue = "true", matchIfMissing = true)
    public RequestEventPublisher rabbitRequestEventPublisher(RabbitTemplate rabbitTemplate) {
        return new RabbitRequestEventPublisher(rabbitTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RequestEventPublisher.class)
    public RequestEventPublisher noOpRequestEventPublisher() {
        return new NoOpRequestEventPublisher();
    }
}
