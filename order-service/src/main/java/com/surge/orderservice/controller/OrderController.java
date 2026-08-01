package com.surge.orderservice.controller;

import com.surge.orderservice.dto.ReservationEvent;
import com.surge.orderservice.entity.Order;
import com.surge.orderservice.repository.OrderRepository;
import com.surge.orderservice.service.OrderService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService; // Inject the refactored service

    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // Dapr POSTs the RabbitMQ message directly to this endpoint
    @Topic(name = "reservation_routing", pubsubName = "surge-pubsub")
    @PostMapping("/reservation_routing")
    public ResponseEntity<Void> processReservation(@RequestBody CloudEvent<ReservationEvent> cloudEvent) {

        // Extract the payload and pass it to your Service layer
        orderService.processReservationEvent(cloudEvent.getData());

        // If the service completes without throwing an exception, return 200 OK to Dapr
        return ResponseEntity.ok().build();
    }
}