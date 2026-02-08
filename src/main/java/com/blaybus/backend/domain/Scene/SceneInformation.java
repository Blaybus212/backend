package com.blaybus.backend.domain.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "scene_information")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SceneInformation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * scene의 기본 배치(alignment) 식별자
	 * - 초기 진입 시 로딩되는 디폴트 배치 상태
	 */
	@Column(name = "default_alignment_id", nullable = false)
	private Long defaultAlignmentId;

	/**
	 * scene 이름
	 */
	@Column(name = "title", length = 55, nullable = false)
	private String title;

	/**
	 * scene 영어 이름
	 */
	@Column(name = "eng_title", length = 55, nullable = false)
	private String engTitle;

	/**
	 * scene 분류값
	 * - 기계공학, 의공학 등 도메인 구분 목적 - 추후 enum 확장
	 */
	@Column(name = "category", length = 20, nullable = false)
	private String category;

	/**
	 * 참석자 수
	 * - scene에 참여 중인 사용자 수를 집계한 값
	 * - 조회 성능을 위한 반정규화 컬럼
	 */
	@Column(name = "participants_count", nullable = false)
	private Long participantsCount;

	/**
	 * scene 개요 설명
	 * - 홈 화면, 상세 진입 전 요약 정보
	 * - 카드 UI 기준 1~2줄을 고려한 길이 제한
	 */
	@Column(name = "description", length = 100)
	private String description;

	/**
	 * scene 썸네일 이미지 URL
	 */
	@Column(name = "thumbnail_url", length = 255)
	private String thumbnailUrl;
}
