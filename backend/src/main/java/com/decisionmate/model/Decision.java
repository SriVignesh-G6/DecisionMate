
package com.decisionmate.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decisions")
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @OneToMany(mappedBy = "decision",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Criterion> criteria = new ArrayList<>();

    @OneToMany(mappedBy = "decision",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Alternative> alternatives = new ArrayList<>();

    public Decision() {
    }

    public Decision(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }
}
