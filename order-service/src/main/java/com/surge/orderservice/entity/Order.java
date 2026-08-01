package com.surge.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NEW: The database will mathematically block duplicates!
    @Column(unique = true, nullable = false)
    private String reservationId;

    private String userId;
    private String eventName;
    private int seatsReserved;
    private String orderStatus;
    private LocalDateTime orderDate;
}