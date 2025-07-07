package com.middleware.backend.kaotocamel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;

public interface DynamicRouteRepository
                extends JpaRepository<DynamicRouteEntity, Long>, JpaSpecificationExecutor<DynamicRouteEntity> {

        List<DynamicRouteEntity> findByRouteIdOrderByVersionDesc(String routeId);

        Optional<DynamicRouteEntity> findByRouteIdAndVersion(String routeId, int version);

        List<DynamicRouteEntity> findByRouteIdAndActiveTrue(String routeId);

        DynamicRouteEntity findByRouteIdAndDefaultVersionTrue(String routeId);

        Page<DynamicRouteEntity> findByActiveTrue(Pageable pageable);

        Page<DynamicRouteEntity> findByRouteId(String routeId, Pageable pageable);
}
