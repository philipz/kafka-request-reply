package com.baeldung.kafka.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import com.baeldung.kafka.synchronous.NotificationDispatchResponse;
import com.baeldung.kafka.synchronous.CalculationResponse;

@Component
class NotificationDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchListener.class);

    @SendTo // Keep @SendTo to automatically reply to the REPLY_TOPIC header
    @KafkaListener(topics = "${com.baeldung.kafka.synchronous.request-topic}") // Update to listen to the intermediate topic (property name is still request-topic in this file)
    NotificationDispatchResponse listen(CalculationResponse request, @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationId) { // Add CORRELATION_ID for logging/tracing
        log.info("Received intermediate request: {} with correlationId {}", request, new String(correlationId));
        
        // Processing logic (Math.sqrt)
        double result = Math.sqrt(request.result()) ;
        NotificationDispatchResponse finalResponse = new NotificationDispatchResponse(result);
        log.info("Sending final response: {}", finalResponse);
        return finalResponse; // @SendTo will handle sending this to the reply topic
    }

} 