package org.belex.paymentschecker.repo;

import org.belex.paymentschecker.modal.Discrepancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long> {
    List<Discrepancy> findByOwnerId(Long ownerId);
    void deleteByOwnerId(Long ownerId);
    List<Discrepancy> findByOwnerIdAndIdIn(Long ownerId, List<Long> ids);
}
