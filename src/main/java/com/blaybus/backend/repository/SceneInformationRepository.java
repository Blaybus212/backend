package com.blaybus.backend.repository;

import com.blaybus.backend.domain.scene.SceneInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SceneInformationRepository extends JpaRepository<SceneInformation, Long> {
}
