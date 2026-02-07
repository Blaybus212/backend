package com.blaybus.backend.domain.conversation;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "conversation", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "user_id", "scene_id" })
})
public class Conversation {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "scene_id", nullable = false, length = 255)
        private Long sceneId;

        /**
         * 현재 대화창 내용의 요약본
         * - LLM 컨텍스트 축약용
         * - 최신 상태만 유지하는 스냅샷 데이터
         * - 원문 대화는 별도 테이블로 관리
         */
        @Lob
        @Column(name = "summary", nullable = false)
        private String summary;
}
