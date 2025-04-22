package com.baeldung.kafka.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.baeldung.kafka.synchronous.CalculationResponse;
import com.baeldung.kafka.synchronous.NotificationDispatchRequest;

@Component
class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    @Autowired
    private KafkaTemplate<String, CalculationResponse> kafkaTemplate;

    @Value("${com.baeldung.kafka.synchronous.intermediate-topic}")
    private String intermediateTopic;

    @KafkaListener(topics = "${com.baeldung.kafka.synchronous.request-topic}") // Listen to the new request topic
    void listen(NotificationDispatchRequest request, 
                @Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTopic, 
                @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationId) { // Add headers to be forwarded
        log.info("Received notification request: {} from topic {} with correlationId {}", request, new String(replyTopic), new String(correlationId));
        
        // Processing logic (Math.pow)
        double result = Math.pow(request.base(), request.exponent()) ;
        CalculationResponse intermediateResponse = new CalculationResponse(result);
        log.info("Sending intermediate response: {} to topic {}", intermediateResponse, intermediateTopic);

        // Build message with intermediate result and forward headers
        Message<CalculationResponse> message = MessageBuilder
                .withPayload(intermediateResponse)
                .setHeader(KafkaHeaders.TOPIC, intermediateTopic) // Set target topic
                .setHeader(KafkaHeaders.REPLY_TOPIC, replyTopic) // Forward reply topic header
                .setHeader(KafkaHeaders.CORRELATION_ID, correlationId) // Forward correlation id header
                // Optional: add a key if needed, e.g., .setHeader(KafkaHeaders.KEY, "someKey")
                .build();

        // Send to intermediate topic
        kafkaTemplate.send(message);
        log.info("Intermediate response sent.");

        // No return value as we are not using @SendTo anymore
    }

} 