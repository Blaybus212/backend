package com.blaybus.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.blaybus.backend.domain.quiz.QuizUserProgress;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.QuizDto;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.QuizUserProgressRepository;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

        @Mock
        private QuizUserProgressRepository progressRepository;

        @InjectMocks
        private QuizService quizService;

        @Test
        @DisplayName("퀴즈 진행 상황을 정상적으로 동기화한다.")
        void syncProgressSuccess() {
                // given
                Long sceneId = 1L;
                User user = mock(User.class);
                given(user.getId()).willReturn(1L);
                SceneInformation scene = SceneInformation.builder().id(sceneId).build();
                QuizUserProgress progress = QuizUserProgress.builder()
                                .id(1L)
                                .user(user)
                                .scene(scene)
                                .totalQuestions(5)
                                .success(2)
                                .failure(1)
                                .solveTime(100)
                                .isComplete(false)
                                .build();

                QuizDto.SyncProgressRequest request = new QuizDto.SyncProgressRequest(
                                3L, 5, 4, 1, 250, true);

                given(progressRepository.findByUserIdAndSceneId(user.getId(), sceneId))
                                .willReturn(Optional.of(progress));

                // when
                quizService.syncProgress(sceneId, request, user);

                // then
                then(progressRepository).should().save(argThat(updated -> updated.getLastQuizId().equals(3L) &&
                                updated.getSuccess().equals(4) &&
                                updated.getSolveTime().equals(250) &&
                                updated.isComplete()));
        }

        @Test
        @DisplayName("진행 상황이 존재하지 않으면 예외를 던진다.")
        void syncProgressNotFound() {
                // given
                Long sceneId = 1L;
                User user = mock(User.class);
                given(user.getId()).willReturn(1L);
                QuizDto.SyncProgressRequest request = new QuizDto.SyncProgressRequest(
                                3L, 5, 4, 1, 250, true);

                given(progressRepository.findByUserIdAndSceneId(user.getId(), sceneId))
                                .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> quizService.syncProgress(sceneId, request, user))
                                .isInstanceOf(BusinessException.class)
                                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.QUIZ_PROGRESS_NOT_FOUND);
        }
}
