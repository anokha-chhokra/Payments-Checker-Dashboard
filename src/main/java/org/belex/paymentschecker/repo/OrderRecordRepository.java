package org.belex.paymentschecker.repo;

import org.belex.paymentschecker.modal.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {
    List<OrderRecord> findByOwnerId(Long ownerId);
    void deleteByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
}
