package com.blaybus.backend.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityResponse {
	private Integer streak;
	private Integer solvedQuizCount;
	private Map<String, CellResponse> cells;
}
