package com.baeldung.kafka.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import com.baeldung.kafka.synchronous.NotificationDispatchResponse;
import com.baeldung.kafka.synchronous.NotificationDispatchRequest;

@Component
class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    @SendTo // Implicitly sends reply to the topic specified in the request's REPLY_TOPIC header
    @KafkaListener(topics = "${com.baeldung.kafka.synchronous.request-topic}") // Use property from application.properties
    NotificationDispatchResponse listen(NotificationDispatchRequest request) {
        log.info("Received notification request: {}", request);
        // Simulate processing logic
        double result = Math.pow(request.base(), request.exponent()) ;
        NotificationDispatchResponse response = new NotificationDispatchResponse(result);
        log.info("Sending notification response: {}", response);
        return response;
    }

} 