package com.decisionmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "alternatives")
public class Alternative {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 800)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    public Alternative() {}
    public Alternative(String name, String description, Decision decision) { this.name = name; this.description = description; this.decision = decision; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Decision getDecision() { return decision; }
}
