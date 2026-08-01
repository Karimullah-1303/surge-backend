package com.surge.inventoryservice.repository;

import com.surge.inventoryservice.entity.InventorySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventorySlotRepository extends JpaRepository<InventorySlot, Long> {

    Optional<InventorySlot> findByEventName(String eventName);
}
