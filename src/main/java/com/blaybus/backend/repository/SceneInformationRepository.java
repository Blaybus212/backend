package com.blaybus.backend.repository;

import com.blaybus.backend.domain.scene.SceneInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SceneInformationRepository extends JpaRepository<SceneInformation, Long> {
    Optional<SceneInformation> findByTitle(String title);
}
