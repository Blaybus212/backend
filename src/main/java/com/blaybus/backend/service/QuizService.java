package com.blaybus.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.quiz.Quiz;
import com.blaybus.backend.domain.quiz.QuizUserProgress;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.QuizResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.QuizRepository;
import com.blaybus.backend.repository.QuizUserProgressRepository;
import com.blaybus.backend.repository.SceneInformationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizService {

	private final QuizRepository quizRepository;
	private final QuizUserProgressRepository progressRepository;
	private final SceneInformationRepository sceneRepository;

	@Transactional
	public QuizResponse getSceneQuizzes(Long sceneId, User user) {
		SceneInformation scene = sceneRepository.findById(sceneId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.SCENE_NOT_FOUND));

		List<Quiz> quizzes = quizRepository.findAllBySceneIdOrderById(sceneId);

		QuizUserProgress progress = progressRepository.findByUserIdAndSceneId(user.getId(), sceneId)
				.orElseGet(() -> {
					QuizUserProgress newProgress = QuizUserProgress.builder()
							.user(user)
							.scene(scene)
							.lastQuizId(null)
							.totalQuestions(quizzes.size())
							.success(0)
							.failure(0)
							.isComplete(false)
							.solveTime(0)
							.build();
					return progressRepository.save(newProgress);
				});

		return mapToResponse(sceneId, progress, quizzes);
	}

	private QuizResponse mapToResponse(Long sceneId, QuizUserProgress progress, List<Quiz> quizzes) {
		QuizResponse.UserProgressDto progressDto = new QuizResponse.UserProgressDto(
				progress.getId(),
				progress.getLastQuizId(),
				progress.getTotalQuestions(),
				progress.getSuccess(),
				progress.getFailure(),
				progress.isComplete());

		List<QuizResponse.QuizItemDto> quizItemDtos = quizzes.stream()
				.map(quiz -> new QuizResponse.QuizItemDto(
						quiz.getId(),
						quiz.getTargetPurpose(),
						quiz.getType(),
						quiz.getQuestion(),
						quiz.getAnswer()))
				.toList();

		return new QuizResponse(sceneId, progressDto, quizItemDtos);
	}
}
