package com.surge.inventoryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.inventoryservice.dto.ReservationEvent;
import com.surge.inventoryservice.entity.InventorySlot;
import com.surge.inventoryservice.entity.OutboxEvent;
import com.surge.inventoryservice.repository.InventorySlotRepository;
import com.surge.inventoryservice.repository.OutboxEventRepository;
import io.dapr.client.DaprClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventorySlotRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final DaprClient daprClient;

    // Instantiating directly removes the IDE autowire error
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String STATE_STORE = "surge-statestore";
    private static final String PUBSUB_NAME = "surge-pubsub";
    private static final String TOPIC_NAME = "reservation_routing";

    public InventorySlot createEvent(String eventName, int totalSeats) {
        InventorySlot slot = new InventorySlot();
        slot.setEventName(eventName);
        slot.setAvailableSeats(totalSeats);
        InventorySlot savedSlot = repository.save(slot);

        daprClient.saveState(STATE_STORE, "inventory:" + eventName, savedSlot).block();
        return savedSlot;
    }

    public InventorySlot getInventoryCacheAside(String eventName) {
        String redisKey = "inventory:" + eventName;

        var state = daprClient.getState(STATE_STORE, redisKey, InventorySlot.class).block();

        if (state != null && state.getValue() != null) {
            InventorySlot cachedSlot = state.getValue();
            if (cachedSlot.getId() != null && cachedSlot.getId() == -1L) {
                throw new RuntimeException("Event not found");
            }
            return cachedSlot;
        }

        Optional<InventorySlot> dbSlotOpt = repository.findByEventName(eventName);

        if (dbSlotOpt.isEmpty()) {
            InventorySlot dummySlot = new InventorySlot();
            dummySlot.setId(-1L);
            daprClient.saveState(STATE_STORE, redisKey, dummySlot).block();
            throw new RuntimeException("Event not found");
        }

        InventorySlot dbSlot = dbSlotOpt.get();
        daprClient.saveState(STATE_STORE, redisKey, dbSlot).block();
        return dbSlot;
    }

    @Transactional
    public InventorySlot reserveTicket(String eventName, int seatsToReserve, String userId) {
        InventorySlot slot = repository.findByEventName(eventName)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (slot.getAvailableSeats() < seatsToReserve) {
            throw new RuntimeException("Not enough seats available!");
        }

        slot.setAvailableSeats(slot.getAvailableSeats() - seatsToReserve);
        InventorySlot updatedSlot = repository.save(slot);

        daprClient.saveState(STATE_STORE, "inventory:" + eventName, updatedSlot).block();

        String uniqueReservationId = UUID.randomUUID().toString();
        ReservationEvent event = new ReservationEvent(uniqueReservationId, eventName, userId, seatsToReserve);

        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateId(uniqueReservationId);
            outboxEvent.setEventType("ReservationEvent");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setStatus("PENDING");
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);
            System.out.println("Saved Reservation to Outbox securely for user: " + userId);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }

        return updatedSlot;
    }
}