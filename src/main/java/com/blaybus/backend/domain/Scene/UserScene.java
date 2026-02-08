package com.blaybus.backend.domain.scene;

import com.blaybus.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * UserScene
 * - 특정 사용자가 3D viewer 에서 보고 있던 scene 상태의 스냅숏
 * - "현재 무엇을 보고 있었는가"에 대한 컨텍스트 저장용 엔티티
 * 포함 정보:
 * - 카메라 시점 (lookAt)
 * - 사용자 노트 (Markdown)
 * 주의:
 * - Scene 자체의 구조나 component 배치 정보는 포함하지 않음
 * - 오직 사용자 관점의 상태만 저장
 */
@Getter
@Builder
@Entity
@Table(name = "user_scene")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * 사용자 카메라 시점 정보
     */
    @Column(name = "look_at", columnDefinition = "jsonb", nullable = false)
    private String lookAt;

    /**
     * 사용자 노트
     * - Markdown 형식의 자유 텍스트
     * - scene에 대한 설명, 학습 메모, 작업 기록 용도
     */
    @Column(name = "note", columnDefinition = "text")
    private String note;

    /**
     * 사용자가 최근에 접속한 기록
     * 최신순 정렬을 위함
     */
    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;
}
