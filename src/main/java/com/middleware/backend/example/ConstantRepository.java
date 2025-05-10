package com.middleware.backend.example;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConstantRepository extends JpaRepository<Constant, String> {
    List<Constant> findAll();
}
