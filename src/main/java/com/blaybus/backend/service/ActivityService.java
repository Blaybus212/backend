package com.blaybus.backend.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.domain.user.UserGrass;
import com.blaybus.backend.dto.ActivityResponse;
import com.blaybus.backend.dto.CellResponse;
import com.blaybus.backend.repository.UserGrassRepository;
import com.blaybus.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

	private final UserGrassRepository userGrassRepository;
	private final UserRepository userRepository;

	public ActivityResponse getMonthlyActivity(Long userId, LocalDate today) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		LocalDate startOfMonth = today.withDayOfMonth(1);
		LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

		List<UserGrass> monthlyData = userGrassRepository.findByUserAndDateBetween(user, startOfMonth, endOfMonth);

		// streak calculation
		int streak = calculateStreak(user, today);

		// monthly solved count
		int solvedCount = monthlyData.stream()
			.mapToInt(UserGrass::getSolvedCount)
			.sum();

		// cells
		Map<String, CellResponse> cells = new LinkedHashMap<>();
		for (UserGrass data : monthlyData) {
			cells.put(data.getDate().toString(), CellResponse.builder()
				.score(data.getScore())
				.level(calculateLevel(data.getScore()))
				.build());
		}

		return ActivityResponse.builder()
			.streak(streak)
			.solvedQuizCount(solvedCount)
			.cells(cells)
			.build();
	}

	private int calculateStreak(User user, LocalDate today) {
		// 1. 오늘자 기록이 있는지 확인
		Optional<UserGrass> todayGrass = userGrassRepository.findByUserAndDate(user, today);
		if (todayGrass.isPresent()) {
			return todayGrass.get().getStreak();
		}

		// 2. 오늘자 기록이 없으면, 가장 최신 기록 하나를 가져옴
		Optional<UserGrass> history = userGrassRepository.findFirstByUserAndDateLessThanEqualOrderByDateDesc(user,
			today.minusDays(1));

		if (history.isEmpty()) {
			return 0;
		}

		UserGrass latest = history.get();

		// 3. 최신 기록이 어제인 경우에만 streak 유지, 그보다 전이면 0
		if (latest.getDate().equals(today.minusDays(1))) {
			return latest.getStreak();
		}

		return 0;
	}

	private int calculateLevel(int score) {
		if (score == 0) {
			return 0;
		}
		if (score <= 10) {
			return 1;
		}
		if (score <= 20) {
			return 2;
		}
		if (score <= 30) {
			return 3;
		}
		return 4;
	}
}
