package com.kapil.tenant.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.KafkaHeaders;


@Component
public class CognosTenantKafkaListener {
    @KafkaListener(topics = "Tenant_Action_Response_Topic", groupId = "utp_group")
    public void listen(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        System.out.println("Received message: " + message + " with key: " + key);

    }
}