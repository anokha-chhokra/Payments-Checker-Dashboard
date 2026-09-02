package org.belex.paymentschecker.repo;

import org.belex.paymentschecker.modal.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByOwnerId(Long ownerId);
    void deleteByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
}
