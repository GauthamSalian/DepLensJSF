package com.deplens.repository;

import com.deplens.model.RepositoryEntity;
import com.deplens.model.RepositoryStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryStateRepository extends JpaRepository<RepositoryStateEntity, Long> {
    Optional<RepositoryStateEntity> findFirstByRepositoryOrderByLastScannedAtDesc(RepositoryEntity repository);
    Optional<RepositoryStateEntity> findFirstByRepositoryAndLatestCommitSha(RepositoryEntity repository, String latestCommitSha);
    List<RepositoryStateEntity> findByRepositoryOrderByLastScannedAtDesc(RepositoryEntity repository);
}
