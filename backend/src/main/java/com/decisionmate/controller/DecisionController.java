package com.decisionmate.controller;

import com.decisionmate.dto.ResultRow;
import com.decisionmate.dto.ScoreRequest;
import com.decisionmate.model.*;
import com.decisionmate.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DecisionController {
    private final DecisionRepository decisions;
    private final CriterionRepository criteria;
    private final AlternativeRepository alternatives;
    private final ScoreRepository scores;

    public DecisionController(DecisionRepository decisions, CriterionRepository criteria,
                              AlternativeRepository alternatives, ScoreRepository scores) {
        this.decisions = decisions; this.criteria = criteria; this.alternatives = alternatives; this.scores = scores;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "application", "DecisionMate API"); }

    @GetMapping("/decisions")
    public List<Map<String, Object>> all() {
        return decisions.findAll().stream().map(this::summary).toList();
    }

    @PostMapping("/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody DecisionRequest request) {
        return summary(decisions.save(new Decision(request.title().trim(), request.description())));
    }

    @GetMapping("/decisions/{id}")
    public Map<String, Object> get(@PathVariable Long id) { return detail(findDecision(id)); }

    @PutMapping("/decisions/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        Decision d = findDecision(id); d.setTitle(request.title().trim()); d.setDescription(request.description());
        return summary(decisions.save(d));
    }

    @DeleteMapping("/decisions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { decisions.delete(findDecision(id)); }

    @PostMapping("/decisions/{id}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addCriterion(@PathVariable Long id, @Valid @RequestBody CriterionRequest request) {
        Decision d = findDecision(id);
        Criterion c = criteria.save(new Criterion(request.name().trim(), request.weight(), d));
        return criterionMap(c);
    }

    @DeleteMapping("/criteria/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCriterion(@PathVariable Long id) {
        Criterion c = criteria.findById(id).orElseThrow(() -> notFound("Criterion"));
        scores.findByCriterionId(id).forEach(scores::delete);
        criteria.delete(c);
    }

    @PostMapping("/decisions/{id}/alternatives")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addAlternative(@PathVariable Long id, @Valid @RequestBody AlternativeRequest request) {
        Alternative a = alternatives.save(new Alternative(request.name().trim(), request.description(), findDecision(id)));
        return alternativeMap(a);
    }

    @DeleteMapping("/alternatives/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlternative(@PathVariable Long id) {
        Alternative a = alternatives.findById(id).orElseThrow(() -> notFound("Alternative"));
        scores.findByAlternativeId(id).forEach(scores::delete);
        alternatives.delete(a);
    }

    @PutMapping("/decisions/{decisionId}/scores")
    @Transactional
    public Map<String, Object> saveScore(@PathVariable Long decisionId, @Valid @RequestBody ScoreRequest request) {
        Decision d = findDecision(decisionId);
        Alternative a = alternatives.findById(request.alternativeId()).orElseThrow(() -> notFound("Alternative"));
        Criterion c = criteria.findById(request.criterionId()).orElseThrow(() -> notFound("Criterion"));
        if (!a.getDecision().getId().equals(d.getId()) || !c.getDecision().getId().equals(d.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternative and criterion must belong to this decision");
        Score score = scores.findByAlternativeIdAndCriterionId(a.getId(), c.getId())
                .orElseGet(() -> new Score(a, c, request.score()));
        score.setScoreValue(request.score());
        Score saved = scores.save(score);
        return Map.of("alternativeId", a.getId(), "criterionId", c.getId(), "score", saved.getScoreValue());
    }

    @GetMapping("/decisions/{id}/results")
    public List<ResultRow> results(@PathVariable Long id) {
        Decision d = findDecision(id);
        List<Criterion> cs = criteria.findByDecisionId(d.getId());
        List<Alternative> as = alternatives.findByDecisionId(d.getId());
        List<Score> allScores = scores.findByAlternativeDecisionId(d.getId());
        Map<String, Integer> scoreMap = allScores.stream().collect(Collectors.toMap(
            s -> s.getAlternative().getId() + ":" + s.getCriterion().getId(), Score::getScoreValue, (a,b) -> b));
        double totalWeight = cs.stream().mapToInt(Criterion::getWeight).sum();
        List<ResultRow> rows = new ArrayList<>();
        for (Alternative a : as) {
            double weighted = 0;
            if (totalWeight > 0) for (Criterion c : cs) {
                Integer value = scoreMap.get(a.getId() + ":" + c.getId());
                if (value != null) weighted += value * c.getWeight();
            }
            double normalized = totalWeight == 0 ? 0 : weighted / totalWeight;
            rows.add(new ResultRow(a.getId(), a.getName(), Math.round(normalized * 100.0) / 100.0, 0));
        }
        rows.sort(Comparator.comparingDouble(ResultRow::weightedScore).reversed().thenComparing(ResultRow::alternativeName));
        List<ResultRow> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ResultRow r = rows.get(i); ranked.add(new ResultRow(r.alternativeId(), r.alternativeName(), r.weightedScore(), i + 1));
        }
        return ranked;
    }

    private Decision findDecision(Long id) {
        return decisions.findById(id).orElseThrow(() -> notFound("Decision"));
    }
    private ResponseStatusException notFound(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found");
    }
    private Map<String,Object> summary(Decision d) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", d.getId()); m.put("title", d.getTitle()); m.put("description", d.getDescription());
        m.put("createdAt", d.getCreatedAt()); m.put("criteriaCount", criteria.findByDecisionId(d.getId()).size());
        m.put("alternativesCount", alternatives.findByDecisionId(d.getId()).size()); return m;
    }
    private Map<String,Object> detail(Decision d) {
        Map<String,Object> m = summary(d);
        m.put("criteria", criteria.findByDecisionId(d.getId()).stream().map(this::criterionMap).toList());
        m.put("alternatives", alternatives.findByDecisionId(d.getId()).stream().map(this::alternativeMap).toList());
        m.put("scores", scores.findByAlternativeDecisionId(d.getId()).stream().map(s -> Map.of(
                "alternativeId", s.getAlternative().getId(),
                "criterionId", s.getCriterion().getId(),
                "score", s.getScoreValue())).toList());
        return m;
    }
    private Map<String,Object> criterionMap(Criterion c) {
        return Map.of("id", c.getId(), "name", c.getName(), "weight", c.getWeight());
    }
    private Map<String,Object> alternativeMap(Alternative a) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id", a.getId()); m.put("name", a.getName());
        m.put("description", a.getDescription()); return m;
    }

    public record DecisionRequest(@NotBlank @Size(max=160) String title, @Size(max=1200) String description) {}
    public record CriterionRequest(@NotBlank @Size(max=120) String name, @NotNull @Min(1) @Max(10) Integer weight) {}
    public record AlternativeRequest(@NotBlank @Size(max=160) String name, @Size(max=800) String description) {}
}
