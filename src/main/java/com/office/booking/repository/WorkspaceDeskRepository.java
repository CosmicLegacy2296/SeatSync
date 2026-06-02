package com.office.booking.repository;

import com.office.booking.model.WorkspaceDesk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceDeskRepository extends JpaRepository<WorkspaceDesk, Long> {
    List<WorkspaceDesk> findByCompanyIdAndFloorNumberAndActiveTrueOrderBySeatIdAsc(String companyId, int floorNumber);

    boolean existsByCompanyIdAndFloorNumberAndSeatIdAndActiveTrue(String companyId, int floorNumber, String seatId);

    long countByCompanyIdAndActiveTrue(String companyId);
}
