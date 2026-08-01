package com.surge.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationEvent implements Serializable {
    private String reservationId; // The unique Idempotency Key
    private String eventName;
    private String userId;
    private int seatsReserved;
}