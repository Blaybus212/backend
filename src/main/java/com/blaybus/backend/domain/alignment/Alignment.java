package com.blaybus.backend.domain.alignment;

import jakarta.persistence.*;
import lombok.*;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.user.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Table(name = "alignments", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"user_id", "scene_id", "component_id"})
})
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Alignment {

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "component_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Component component;

	@Column(name = "transform_matrix", columnDefinition = "json", nullable = false)
	private String transformMatrix;
}
