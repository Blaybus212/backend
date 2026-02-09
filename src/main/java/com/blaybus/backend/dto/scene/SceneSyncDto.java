package com.blaybus.backend.dto.scene;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneSyncDto {
	private List<ComponentStateDto> components;
	private Map<String, Object> lookAt;
}
