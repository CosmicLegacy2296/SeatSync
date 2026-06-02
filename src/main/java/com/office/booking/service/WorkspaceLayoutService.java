package com.office.booking.service;

import com.office.booking.model.Company;
import com.office.booking.model.Floor;
import com.office.booking.model.WorkspaceDesk;
import com.office.booking.model.WorkspaceFloor;
import com.office.booking.repository.WorkspaceDeskRepository;
import com.office.booking.repository.WorkspaceFloorRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceLayoutService {
    private final WorkspaceFloorRepository floorRepository;
    private final WorkspaceDeskRepository deskRepository;
    private final CompanyService companyService;

    public WorkspaceLayoutService(
            WorkspaceFloorRepository floorRepository,
            WorkspaceDeskRepository deskRepository,
            CompanyService companyService) {
        this.floorRepository = floorRepository;
        this.deskRepository = deskRepository;
        this.companyService = companyService;
    }

    public void ensureLayoutForCompany(Company company) {
        if (company == null || company.getId() == null || company.getId().isBlank()) {
            return;
        }
        if (floorRepository.existsByCompanyId(company.getId())) {
            return;
        }

        Map<Integer, Integer> floorSeats = parseFloorSeatConfig(company);

        // Keep ground floor for UI compatibility.
        floorRepository.save(new WorkspaceFloor(company.getId(), 0, false));

        for (Map.Entry<Integer, Integer> entry : floorSeats.entrySet()) {
            int floorNumber = entry.getKey();
            int seatCount = Math.max(0, entry.getValue());
            if (floorNumber <= 0) {
                continue;
            }

            floorRepository.save(new WorkspaceFloor(company.getId(), floorNumber, seatCount > 0));
            for (int i = 1; i <= seatCount; i++) {
                String seatId = "F" + floorNumber + "-S" + i;
                deskRepository.save(new WorkspaceDesk(company.getId(), floorNumber, seatId, true));
            }
        }
    }

    public void ensureLayoutForCompanyId(String companyId) {
        if (companyId == null || companyId.isBlank() || floorRepository.existsByCompanyId(companyId)) {
            return;
        }
        companyService.findById(companyId).ifPresent(this::ensureLayoutForCompany);
    }

    public List<Floor> getFloorsForCompany(String companyId) {
        ensureLayoutForCompanyId(companyId);

        List<WorkspaceFloor> floors = floorRepository.findByCompanyIdOrderByFloorNumberAsc(companyId);
        List<Floor> result = new ArrayList<>();
        for (WorkspaceFloor f : floors) {
            Floor floor = new Floor();
            floor.setFloorNumber(f.getFloorNumber());
            floor.setHasSeats(f.isHasSeats());

            if (f.isHasSeats()) {
                List<String> seats = deskRepository
                        .findByCompanyIdAndFloorNumberAndActiveTrueOrderBySeatIdAsc(companyId, f.getFloorNumber())
                        .stream()
                        .map(WorkspaceDesk::getSeatId)
                        .toList();
                floor.setSeats(new ArrayList<>(seats));
            } else {
                floor.setSeats(new ArrayList<>());
            }

            result.add(floor);
        }
        return result;
    }

    public boolean isValidSeat(String companyId, int floorNumber, String seatId) {
        ensureLayoutForCompanyId(companyId);
        return deskRepository.existsByCompanyIdAndFloorNumberAndSeatIdAndActiveTrue(companyId, floorNumber, seatId);
    }

    public long countDesks(String companyId) {
        ensureLayoutForCompanyId(companyId);
        return deskRepository.countByCompanyIdAndActiveTrue(companyId);
    }

    public long countSeatFloors(String companyId) {
        ensureLayoutForCompanyId(companyId);
        return floorRepository.countByCompanyIdAndHasSeatsTrue(companyId);
    }

    private Map<Integer, Integer> parseFloorSeatConfig(Company company) {
        Map<Integer, Integer> map = new LinkedHashMap<>();

        String raw = company.getFloorSeatConfig();
        if (raw != null && !raw.isBlank()) {
            try {
                String clean = raw.trim().replaceAll("[{}\"]", "");
                if (!clean.isBlank()) {
                    for (String entry : clean.split(",")) {
                        String[] kv = entry.trim().split(":");
                        if (kv.length == 2) {
                            int floor = Integer.parseInt(kv[0].trim());
                            int seats = Integer.parseInt(kv[1].trim());
                            map.put(floor, Math.max(0, seats));
                        }
                    }
                }
            } catch (Exception ignored) {
                // Fall back to legacy fields below.
            }
        }

        if (map.isEmpty()) {
            map.put(1, Math.max(1, company.getFloor1Seats()));
            map.put(2, Math.max(1, company.getFloor2Seats()));
        }

        return map.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }
}
