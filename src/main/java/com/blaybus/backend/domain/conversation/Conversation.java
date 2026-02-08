package com.blaybus.backend.domain.conversation;

import jakarta.persistence.*;
import lombok.*;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Builder
@Entity
@Table(name = "conversation", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "user_id", "scene_id" })
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
        @Column(name = "summary", nullable = false)
        private String summary;
}
