package com.surge.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationEvent implements Serializable {
    private String reservationId;
    private String eventName;
    private String userId;
    private int seatsReserved;
}
