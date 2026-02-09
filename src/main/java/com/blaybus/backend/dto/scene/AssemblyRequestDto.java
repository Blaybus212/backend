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
public class AssemblyRequestDto {
	private List<AssemblyNodeDto> instances;
	private Map<String, String> assets;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AssemblyNodeDto {
		private String name;
		private List<Double> matrix;
		private String assetId;
		private Map<String, Object> extras;
	}
}
