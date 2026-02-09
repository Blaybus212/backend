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
public class SceneAssemblyDto {
	private String file;
	private List<SceneNodeDto> nodes;
}
