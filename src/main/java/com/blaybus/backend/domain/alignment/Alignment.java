package com.blaybus.backend.domain.alignment;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "alignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"user_id", "scene_id", "component_id"}
                )
        })
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Alignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "component_id", nullable = false)
    private Long componentId;

    @Column(
            name = "transform_matrix",
            columnDefinition = "json",
            nullable = false
    )
    private String transformMatrix;
}
