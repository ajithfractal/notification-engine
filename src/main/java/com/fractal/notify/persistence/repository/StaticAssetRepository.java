package com.fractal.notify.persistence.repository;

import com.fractal.notify.persistence.entity.StaticAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StaticAssetEntity.
 */
@Repository
public interface StaticAssetRepository extends JpaRepository<StaticAssetEntity, Long> {
    
    Optional<StaticAssetEntity> findByNameAndIsActiveTrue(String name);
    
    Optional<StaticAssetEntity> findByName(String name);
    
    List<StaticAssetEntity> findByIsActiveTrue();
    
    Optional<StaticAssetEntity> findByContentIdAndIsActiveTrue(String contentId);
}
