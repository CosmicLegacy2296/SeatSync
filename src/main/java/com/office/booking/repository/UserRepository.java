package com.office.booking.repository;

import com.office.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByCompanyId(String companyId);

    Optional<User> findByDisplayName(String displayName);

    boolean existsByDisplayName(String displayName);
}

