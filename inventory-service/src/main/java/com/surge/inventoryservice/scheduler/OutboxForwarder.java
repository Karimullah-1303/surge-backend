package com.surge.inventoryservice.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.inventoryservice.dto.ReservationEvent;
import com.surge.inventoryservice.entity.OutboxEvent;
import com.surge.inventoryservice.repository.OutboxEventRepository;
import io.dapr.client.DaprClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxForwarder {

    private final OutboxEventRepository outboxEventRepository;
    private final DaprClient daprClient;

    // Instantiating directly removes the IDE autowire error
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PUBSUB_NAME = "surge-pubsub";
    private static final String TOPIC_NAME = "reservation_routing";

    @Scheduled(fixedDelay = 2000)
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                ReservationEvent payload = objectMapper.readValue(outboxEvent.getPayload(), ReservationEvent.class);

                daprClient.publishEvent(PUBSUB_NAME, TOPIC_NAME, payload).block();
                System.out.println("Outbox Forwarder: Published event " + outboxEvent.getAggregateId() + " to Dapr.");

                outboxEvent.setStatus("PROCESSED");
                outboxEventRepository.save(outboxEvent);

            } catch (JsonProcessingException e) {
                System.err.println("Failed to process outbox event ID: " + outboxEvent.getId());
            }
        }
    }
}