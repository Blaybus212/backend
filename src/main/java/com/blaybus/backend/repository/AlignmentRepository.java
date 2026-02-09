package com.blaybus.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.blaybus.backend.domain.alignment.Alignment;

@Repository
public interface AlignmentRepository extends JpaRepository<Alignment, Long> {
	List<Alignment> findByUserIdAndSceneId(Long userId, Long sceneId);

	Optional<Alignment> findByUserIdAndSceneIdAndNodeName(Long userId, Long sceneId, String nodeName);
}
