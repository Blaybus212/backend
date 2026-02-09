package com.blaybus.backend.domain.user;

import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "user_grass", uniqueConstraints = {
	// 한 사용자는 하루에 하나의 grass 데이터만 가져야 함
	@UniqueConstraint(columnNames = {"user_id", "date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGrass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	/**
	 * 점수가 집계된 날짜
	 * - 일 단위 grass 표현을 위함
	 * - 기간 조회 (주간 / 월간 / 연간)에 사용
	 */
	@Column(name = "date", nullable = false)
	private LocalDate date;

	/**
	 * 해당 날짜에 사용자가 누적한 점수
	 * - 커밋 수, 학습량, 활동량 등으로 환산 가능
	 * - 잔디 색상/크기 계산의 기준 값
	 */
	@Column(name = "score", nullable = false)
	private Integer score;
}
