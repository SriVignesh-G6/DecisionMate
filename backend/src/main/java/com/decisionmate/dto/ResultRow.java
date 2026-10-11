package com.decisionmate.dto;
public record ResultRow(Long alternativeId, String alternativeName, double weightedScore, int rank) {}
