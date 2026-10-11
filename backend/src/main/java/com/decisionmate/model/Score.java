package com.decisionmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "scores", uniqueConstraints = @UniqueConstraint(columnNames = {"alternative_id", "criterion_id"}))
public class Score {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "alternative_id", nullable = false)
    private Alternative alternative;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;
    @Min(1) @Max(10) @Column(nullable = false)
    private Integer scoreValue;

    public Score() {}
    public Score(Alternative alternative, Criterion criterion, Integer scoreValue) {
        this.alternative = alternative; this.criterion = criterion; this.scoreValue = scoreValue;
    }
    public Long getId() { return id; }
    public Alternative getAlternative() { return alternative; }
    public Criterion getCriterion() { return criterion; }
    public Integer getScoreValue() { return scoreValue; }
    public void setScoreValue(Integer scoreValue) { this.scoreValue = scoreValue; }
}
