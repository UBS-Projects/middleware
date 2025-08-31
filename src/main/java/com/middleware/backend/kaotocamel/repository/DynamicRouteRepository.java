package com.middleware.backend.kaotocamel.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;

public interface DynamicRouteRepository
        extends JpaRepository<DynamicRouteEntity, Long>, JpaSpecificationExecutor<DynamicRouteEntity> {

        List<DynamicRouteEntity> findByRouteIdOrderByVersionDesc(String routeId);

        Optional<DynamicRouteEntity> findByRouteIdAndVersion(String routeId, int version);

//        List<DynamicRouteEntity> findByRouteIdAndActiveTrue(String routeId);

        DynamicRouteEntity findByRouteIdAndDefaultVersionTrue(String routeId);

        Page<DynamicRouteEntity> findByActiveTrue(Pageable pageable);
        List<DynamicRouteEntity> findByActiveTrue();

        Page<DynamicRouteEntity> findByRouteId(String routeId, Pageable pageable);

        @Query("SELECT COUNT(DISTINCT r.routeId) FROM DynamicRouteEntity r")
        long countDistinctRouteIds();

        @Query("SELECT r FROM DynamicRouteEntity r ORDER BY r.createdAt DESC")
        List<DynamicRouteEntity> findAllOrderByCreatedAtDesc();
        // أضف هذه الـ Methods لـ DynamicRouteRepository الموجود عندك

        @Query("SELECT dr FROM DynamicRouteEntity dr WHERE dr.active = true AND dr.defaultVersion = true ORDER BY dr.routeId")
        List<DynamicRouteEntity> findLatestActiveRoutes();

        @Query("SELECT dr FROM DynamicRouteEntity dr WHERE dr.routeId = :routeId AND dr.active = true")
        List<DynamicRouteEntity> findByRouteIdAndActiveTrue(@Param("routeId") String routeId);

        @Query("SELECT COUNT(dr) > 0 FROM DynamicRouteEntity dr WHERE dr.routeId = :routeId AND dr.active = true")
        boolean existsByRouteIdAndActiveTrue(@Param("routeId") String routeId);


        Optional<DynamicRouteEntity> findByPathAndActive(String path,boolean active);

        Optional<DynamicRouteEntity> findFirstByRouteIdAndActiveTrue(String routeId);

        // Get all distinct routeIds
        @Query("SELECT DISTINCT r.routeId FROM DynamicRouteEntity r")
        List<String> findAllDistinctRouteIds();

}