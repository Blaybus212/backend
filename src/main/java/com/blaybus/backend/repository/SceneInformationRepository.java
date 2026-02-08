package com.blaybus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.scene.SceneInformation;

public interface SceneInformationRepository extends JpaRepository<SceneInformation, Long> {
}
