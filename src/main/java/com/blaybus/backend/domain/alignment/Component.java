package com.blaybus.backend.domain.alignment;

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
@Table(name = "component")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Component {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 사용자에게 표시될 부품 이름
	 * 예: "왼쪽 로봇 암", "기어 A"
	 */
	@Column(name = "name", length = 255, nullable = false)
	private String name;

	/**
	 * 부품에 대한 일반화된 설명
	 * 용도, 특징 등을 자유 텍스트로 저장
	 */
	@Column(name = "description", length = 500)
	private String description;

	/**
	 * 텍스처 또는 외형 관련 키워드
	 * raw text 형태로 저장 (정규화하지 않음)
	 * 예: "레몬, 키위, 자두"
	 */
	@Column(name = "texture", length = 255)
	private String texture;

	/**
	 * 부품 사용 목적 또는 활용 맥락
	 * raw text 형태
	 * 예: "제조, 조립, 용접"
	 */
	@Column(name = "usage", length = 255)
	private String usage;

	/**
	 * GLTF 에셋 파일 경로 (상대 경로 또는 파일명)
	 * 예: "Arm gear.gltf"
	 */
	@Column(name = "asset_path")
	private String assetPath;
}
