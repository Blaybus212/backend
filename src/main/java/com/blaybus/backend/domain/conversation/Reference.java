package com.blaybus.backend.domain.conversation;

import jakarta.persistence.*;
import lombok.*;

import com.blaybus.backend.domain.alignment.Component;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Builder
@Entity
@Table(name = "reference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 참조가 포함된 메세지의 식별자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Message message;

    /**
     * 메세지 내에서 가리키는 실제 대상(component)의 식별자
     * 예: {{object_id}} → component_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Component component;
}
