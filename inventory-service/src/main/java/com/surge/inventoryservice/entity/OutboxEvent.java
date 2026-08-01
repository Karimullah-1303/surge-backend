package com.surge.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId; // e.g., the Reservation ID
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload; // The JSON representation of the event

    private String status; // PENDING or PROCESSED

    private LocalDateTime createdAt;
}