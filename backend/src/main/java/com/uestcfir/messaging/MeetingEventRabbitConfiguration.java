package com.uestcfir.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@ConditionalOnProperty(prefix = "meeting.events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MeetingEventRabbitConfiguration {

    @Bean
    public TopicExchange meetingEventsExchange(MeetingEventProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public TopicExchange meetingEventsDeadLetterExchange(MeetingEventProperties properties) {
        return new TopicExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue meetingNotificationEventsQueue(MeetingEventProperties properties) {
        return QueueBuilder.durable(properties.getQueue())
                .deadLetterExchange(properties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue meetingNotificationEventsDeadLetterQueue(MeetingEventProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding meetingEventsBinding(
            @Qualifier("meetingNotificationEventsQueue") Queue meetingNotificationEventsQueue,
            @Qualifier("meetingEventsExchange") TopicExchange meetingEventsExchange
    ) {
        return BindingBuilder.bind(meetingNotificationEventsQueue)
                .to(meetingEventsExchange)
                .with("#");
    }

    @Bean
    public Binding meetingEventsDeadLetterBinding(
            @Qualifier("meetingNotificationEventsDeadLetterQueue") Queue meetingNotificationEventsDeadLetterQueue,
            @Qualifier("meetingEventsDeadLetterExchange") TopicExchange meetingEventsDeadLetterExchange,
            MeetingEventProperties properties
    ) {
        return BindingBuilder.bind(meetingNotificationEventsDeadLetterQueue)
                .to(meetingEventsDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }
}
