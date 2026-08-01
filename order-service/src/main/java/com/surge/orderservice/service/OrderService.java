package com.surge.orderservice.service;

import com.surge.orderservice.dto.ReservationEvent;
import com.surge.orderservice.entity.Order;
import com.surge.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // The @RabbitListener annotation is gone.
    // This is now a standard Java method that your Dapr-enabled Controller will call.
    public void processReservationEvent(ReservationEvent event) {
        System.out.println(">>> Message Received via Dapr! Processing order: " + event.getReservationId());

        try {
            // OPTIONAL IDEMPOTENCY PRE-CHECK:
            if (orderRepository.existsByReservationId(event.getReservationId())) {
                System.out.println(">>> DUPLICATE DETECTED (Pre-check): Order " + event.getReservationId() + " already exists. Dropping message.");
                return; // Return normally so the Controller sends a 200 OK back to Dapr
            }

            Order order = new Order();
            order.setReservationId(event.getReservationId());
            order.setUserId(event.getUserId());
            order.setEventName(event.getEventName());
            order.setSeatsReserved(event.getSeatsReserved());
            order.setOrderStatus("CONFIRMED");
            order.setOrderDate(LocalDateTime.now());

            orderRepository.save(order);
            System.out.println(">>> Order successfully saved to DB for user: " + event.getUserId());

        } catch (DataIntegrityViolationException e) {
            // IDEMPOTENCY DEFENSE: Postgres blocked the duplicate ID!
            System.out.println(">>> DUPLICATE DETECTED (DB Constraint): Order " + event.getReservationId() + " already exists. Dropping message.");
            // Swallow the exception so the Controller sends a 200 OK. Dapr will consider it handled and delete it from RabbitMQ.

        } catch (Exception e) {
            // POISON PILL DEFENSE: Something else went wrong.
            System.err.println(">>> ERROR: Failed to process message. Throwing exception to trigger Dapr retry mechanism.");
            // Re-throw the exception. The Controller will return a 5xx error, and Dapr will automatically retry the message.
            throw e;
        }
    }
}