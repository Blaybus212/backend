package com.blaybus.backend.domain.conversation;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "reference")
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 참조가 포함된 메세지의 식별자
     */
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    /**
     * 메세지 내에서 가리키는 실제 대상(component)의 식별자
     * 예: {{object_id}} → component_id
     */
    @Column(name = "component_id", nullable = false)
    private Long componentId;
}
