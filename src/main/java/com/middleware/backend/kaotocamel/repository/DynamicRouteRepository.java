package com.middleware.backend.kaotocamel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;

public interface DynamicRouteRepository extends JpaRepository<DynamicRouteEntity, Long> {

    List<DynamicRouteEntity> findByRouteIdOrderByVersionDesc(String routeId);

    Optional<DynamicRouteEntity> findByRouteIdAndVersion(String routeId, int version);

    List<DynamicRouteEntity> findByRouteIdAndActiveTrue(String routeId);

    Page<DynamicRouteEntity> findByActiveTrue(Pageable pageable);

    Page<DynamicRouteEntity> findByRouteId(String routeId, Pageable pageable);
}
