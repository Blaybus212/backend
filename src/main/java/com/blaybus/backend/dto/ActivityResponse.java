package com.blaybus.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ActivityResponse {
    private Integer streak;
    private Integer solvedQuizCount;
    private Map<String, CellResponse> cells;
}
