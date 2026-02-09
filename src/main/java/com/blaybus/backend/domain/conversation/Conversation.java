package com.blaybus.backend.domain.conversation;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@Table(name = "conversation", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"user_id", "scene_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scene_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private SceneInformation scene;

	/**
	 * 현재 대화창 내용의 요약본
	 * - LLM 컨텍스트 축약용
	 * - 최신 상태만 유지하는 스냅샷 데이터
	 * - 원문 대화는 별도 테이블로 관리
	 */
	@Lob
	@Column(name = "summary", nullable = true)
	private String summary;

	public void updateSummary(String summary) {
		this.summary = summary;
	}
}
