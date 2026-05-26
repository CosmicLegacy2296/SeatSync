package com.office.booking.repository;

import com.office.booking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);

    List<User> findByCompanyId(String companyId);
}

