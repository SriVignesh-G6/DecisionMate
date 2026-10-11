package com.decisionmate.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public record ScoreRequest(@NotNull Long alternativeId, @NotNull Long criterionId,
                           @NotNull @Min(1) @Max(10) Integer score) {}
