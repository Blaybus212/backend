package com.blaybus.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.blaybus.backend.domain.scene.UserScene;

@Repository
public interface UserSceneRepository extends JpaRepository<UserScene, Long> {
	List<UserScene> findByUserId(Long userId);

	Optional<UserScene> findByUserIdAndSceneId(Long userId, Long sceneId);
}
