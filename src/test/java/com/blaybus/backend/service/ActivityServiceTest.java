package com.blaybus.backend.service;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.domain.user.UserGrass;
import com.blaybus.backend.dto.ActivityResponse;
import com.blaybus.backend.repository.UserGrassRepository;
import com.blaybus.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

	@Mock
	private UserGrassRepository userGrassRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ActivityService activityService;

	@Test
	@DisplayName("사용자의 이번 달 활동량을 정확하게 집계한다.")
	void getMonthlyActivityTest() {
		// given
		Long userId = 1L;
		User user = User.builder().username("test").build();
		LocalDate today = LocalDate.of(2026, 2, 9);
		LocalDate startOfMonth = today.withDayOfMonth(1);
		LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

		UserGrass g1 = UserGrass.builder().date(LocalDate.of(2026, 2, 9)).score(15).solvedCount(5).streak(3)
			.build();
		UserGrass g2 = UserGrass.builder().date(LocalDate.of(2026, 2, 8)).score(10).solvedCount(5).streak(2)
			.build();
		UserGrass g3 = UserGrass.builder().date(LocalDate.of(2026, 2, 1)).score(25).solvedCount(10).streak(1)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(userGrassRepository.findByUserAndDateBetween(user, startOfMonth, endOfMonth))
			.willReturn(List.of(g1, g2, g3));

		// for streak (today)
		given(userGrassRepository.findByUserAndDate(user, today)).willReturn(Optional.of(g1));

		// when
		ActivityResponse response = activityService.getMonthlyActivity(userId, today);

		// then
		assertThat(response.getStreak()).isEqualTo(3);
		assertThat(response.getSolvedQuizCount()).isEqualTo(20); // 5 + 5 + 10
		assertThat(response.getCells()).hasSize(3);
		assertThat(response.getCells().get("2026-02-09").getLevel()).isEqualTo(2); // score 15
		assertThat(response.getCells().get("2026-02-08").getLevel()).isEqualTo(1); // score 10
		assertThat(response.getCells().get("2026-02-01").getLevel()).isEqualTo(3); // score 25
	}

	@Test
	@DisplayName("연속 학습 일수(streak)를 정확하게 가져온다. (오늘 기록이 없는 경우 어제 기록 사용)")
	void calculateStreakWithInactiveTodayTest() {
		// given
		Long userId = 1L;
		User user = User.builder().username("test").build();
		LocalDate today = LocalDate.of(2026, 2, 9);
		LocalDate yesterday = today.minusDays(1);

		UserGrass gYesterday = UserGrass.builder().date(yesterday).score(10).solvedCount(5).streak(5).build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(userGrassRepository.findByUserAndDateBetween(eq(user), any(), any()))
			.willReturn(List.of(gYesterday));

		// today grass not found
		given(userGrassRepository.findByUserAndDate(user, today)).willReturn(Optional.empty());
		// find most recent before today
		given(userGrassRepository.findFirstByUserAndDateLessThanEqualOrderByDateDesc(user, yesterday))
			.willReturn(Optional.of(gYesterday));

		// when
		ActivityResponse response = activityService.getMonthlyActivity(userId, today);

		// then
		assertThat(response.getStreak()).isEqualTo(5); // Streak from yesterday maintained
	}
}
