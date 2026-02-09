package com.blaybus.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CellResponse {
    private Integer score;
    private Integer level;
}
