package com.office.booking.repository;

import com.office.booking.model.WorkspaceFloor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceFloorRepository extends JpaRepository<WorkspaceFloor, Long> {
    boolean existsByCompanyId(String companyId);

    List<WorkspaceFloor> findByCompanyIdOrderByFloorNumberAsc(String companyId);

    long countByCompanyIdAndHasSeatsTrue(String companyId);
}
