package com.decisionmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "criteria")
public class Criterion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 120)
    private String name;
    @Min(1) @Max(10) @Column(nullable = false)
    private Integer weight = 1;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    public Criterion() {}
    public Criterion(String name, Integer weight, Decision decision) { this.name = name; this.weight = weight; this.decision = decision; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Decision getDecision() { return decision; }
}
