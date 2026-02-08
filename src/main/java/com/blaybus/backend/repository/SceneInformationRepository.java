package com.blaybus.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.blaybus.backend.domain.scene.SceneInformation;

@Repository
public interface SceneInformationRepository extends JpaRepository<SceneInformation, Long> {
	
  Optional<SceneInformation> findByTitle(String title);
}