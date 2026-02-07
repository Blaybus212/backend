package com.blaybus.backend.domain.conversation;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Builder

@Entity
@Table(name = "message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Message {

    /**
     * 메세지 자체 식별자 (Surrogate Key)
     * 대화 흐름 정렬 및 페이징 기준으로 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속된 대화(conversation)의 식별자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Conversation conversation;

    /**
     * 발신자 구분
     * 허용 값: "USER", "ASSISTANT"
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender", length = 11, nullable = false)
    private Sender sender;

    /**
     * 정제(sanitize)된 메세지 본문
     * LLM 입력 및 로그 보관 용도
     * 예: "{{object_id}}는 어디까지 올라가요?"
     */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * 메세지 생성 시각
     * 사용자가 메세지를 보낸 시점 또는
     * assistant 응답이 생성된 시점
     */
    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt;
}
