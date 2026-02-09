package com.blaybus.backend.dto.scene;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneNodeDto {
	private String name;
	private List<Double> matrix;
	private List<Integer> children;
}
