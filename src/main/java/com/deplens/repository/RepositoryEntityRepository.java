package com.deplens.repository;

import com.deplens.model.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryEntityRepository extends JpaRepository<RepositoryEntity, Long> {
    Optional<RepositoryEntity> findFirstByFullNameIgnoreCase(String fullName);
    List<RepositoryEntity> findByOwnerNameIgnoreCase(String ownerName);
}
