package com.blaybus.backend.dto.scene;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SceneConfigDto {
	private Map<String, String> assets;
	private List<InstanceDto> instances;

	@Getter
	@Builder
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InstanceDto {
		private String name; // nodeName (노드 이름)
		private String assetId; // componentName key (컴포넌트 식별자)
		private List<Double> matrix;
		private Map<String, Object> extras;
	}
}
