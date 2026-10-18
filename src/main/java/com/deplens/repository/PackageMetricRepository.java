package com.deplens.repository;

import com.deplens.model.DependencyGraphEntity;
import com.deplens.model.PackageMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageMetricRepository extends JpaRepository<PackageMetricEntity, Long> {
    List<PackageMetricEntity> findByGraphOrderByCompositeRiskScoreDesc(DependencyGraphEntity graph);
}
