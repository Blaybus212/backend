package com.blaybus.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.quiz.Quiz;

/**
 * QuizRepository provides CRUD operations for Quiz entities.
 */
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByQuestion(String question);

    List<Quiz> findAllBySceneId(Long sceneId);
}
