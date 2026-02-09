package com.blaybus.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.quiz.QuizType;
import com.blaybus.backend.domain.quiz.QuizUserProgress;
import com.blaybus.backend.domain.quiz.Quiz;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.QuizDto;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.QuizRepository;
import com.blaybus.backend.repository.QuizUserProgressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizGradingService {

	private static final double SIMILARITY_THRESHOLD = 0.8;

	private final QuizRepository quizRepository;
	private final QuizUserProgressRepository progressRepository;
	private final EmbeddingService embeddingService;

	@Transactional
	public QuizDto.GradeResponse grade(Long quizId, String userAnswer, User user) {
		Quiz quiz = quizRepository.findById(quizId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.QUIZ_NOT_FOUND));

		boolean correct;
		double score;

		if (quiz.getType() == QuizType.SELECT) {
			correct = quiz.getAnswer().split("|")[0].equalsIgnoreCase(userAnswer.trim());
			score = correct ? 1.0 : 0.0;
		} else {
			score = embeddingService.calculateSimilarity(userAnswer, quiz.getAnswer());
			correct = score >= SIMILARITY_THRESHOLD;
		}

		updateProgress(user, quiz, correct);

		return new QuizDto.GradeResponse(correct, score, quiz.getAnswer());
	}

	private void updateProgress(User user, Quiz quiz, boolean correct) {
		QuizUserProgress progress = progressRepository
				.findByUserIdAndSceneId(user.getId(), quiz.getScene().getId())
				.orElseGet(() -> QuizUserProgress.builder()
						.user(user)
						.scene(quiz.getScene())
						.lastQuizId(quiz.getId())
						.totalQuestions(0)
						.success(0)
						.failure(0)
						.isComplete(false)
						.solveTime(0)
						.build());

		QuizUserProgress updated = QuizUserProgress.builder()
				.id(progress.getId())
				.user(progress.getUser())
				.scene(progress.getScene())
				.lastQuizId(quiz.getId())
				.totalQuestions(progress.getTotalQuestions() + 1)
				.success(correct ? progress.getSuccess() + 1 : progress.getSuccess())
				.failure(correct ? progress.getFailure() : progress.getFailure() + 1)
				.isComplete(progress.isComplete())
				.solveTime(progress.getSolveTime())
				.build();

		progressRepository.save(updated);
	}
}
