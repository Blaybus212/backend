package com.blaybus.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.quiz.SceneRanksQuiz;

public interface QuizRepository extends JpaRepository<SceneRanksQuiz, Long> {
}
