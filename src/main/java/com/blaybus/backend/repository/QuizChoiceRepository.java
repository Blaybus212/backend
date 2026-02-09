package com.blaybus.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.quiz.SceneRanksQuizChoice;

public interface QuizChoiceRepository extends JpaRepository<SceneRanksQuizChoice, Long> {
	List<SceneRanksQuizChoice> findByQuizIdOrderByOrderIndex(Long quizId);
}
