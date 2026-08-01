package com.surge.inventoryservice.controller;

import com.surge.inventoryservice.entity.InventorySlot;
import com.surge.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<?> createInventory(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam String eventName,
            @RequestParam int totalSeats){

        if(!"ROLE_ADMIN".equals(role)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        InventorySlot newEvent = inventoryService.createEvent(eventName, totalSeats);
        return ResponseEntity.ok(newEvent);
    }

    @GetMapping("/{eventName}")
    public ResponseEntity<?> checkInventory(@PathVariable String eventName){
        try{
            InventorySlot slot = inventoryService.getInventoryCacheAside(eventName);
            return ResponseEntity.ok(slot);
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{eventName}/reserve")
    public ResponseEntity<?> reserveTicket(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String eventName,
            @RequestParam(defaultValue = "1") int seats) {

        try {
            InventorySlot updatedSlot = inventoryService.reserveTicket(eventName, seats, userId);
            return ResponseEntity.ok("Successfully reserved " + seats + " seats for user " + userId + "! Remaining: " + updatedSlot.getAvailableSeats());

        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Server busy! Someone else just grabbed those tickets. Please retry.");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testGatewayRouting(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        String responseMessage = String.format(
                "Inventory Service Reached! Welcome %s (Role: %s).",
                userId, role
        );

        return ResponseEntity.ok(responseMessage);
    }
}