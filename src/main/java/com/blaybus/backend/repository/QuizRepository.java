package com.blaybus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.quiz.Quiz;

/**
 * QuizRepository provides CRUD operations for Quiz entities.
 */
public interface QuizRepository extends JpaRepository<Quiz, Long> {
}
