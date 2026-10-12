
package com.decisionmate.controller;

import com.decisionmate.dto.ResultRow;
import com.decisionmate.dto.ScoreRequest;
import com.decisionmate.model.*;
import com.decisionmate.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    private final AppUserRepository users;

    public DecisionController(
            DecisionRepository decisions,
            CriterionRepository criteria,
            AlternativeRepository alternatives,
            ScoreRepository scores,
            AppUserRepository users) {
        this.decisions = decisions;
        this.criteria = criteria;
        this.alternatives = alternatives;
        this.scores = scores;
        this.users = users;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "application", "DecisionMate API"
        );
    }

    @GetMapping("/decisions")
    public List<Map<String, Object>> all(Authentication authentication) {
        AppUser user = currentUser(authentication);

        return decisions.findAllByOwner_Id(user.getId())
                .stream()
                .map(this::summary)
                .toList();
    }

    @PostMapping("/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
            @Valid @RequestBody DecisionRequest request,
            Authentication authentication) {

        AppUser user = currentUser(authentication);

        Decision decision = new Decision(
                request.title().trim(),
                request.description()
        );
        decision.setOwner(user);

        return summary(decisions.save(decision));
    }

    @GetMapping("/decisions/{id}")
    public Map<String, Object> get(
            @PathVariable Long id,
            Authentication authentication) {
        return detail(findDecision(id, currentUser(authentication)));
    }

    @PutMapping("/decisions/{id}")
    public Map<String, Object> update(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request,
            Authentication authentication) {

        Decision decision = findDecision(id, currentUser(authentication));
        decision.setTitle(request.title().trim());
        decision.setDescription(request.description());

        return summary(decisions.save(decision));
    }

    @DeleteMapping("/decisions/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        Decision decision = findDecision(id, currentUser(authentication));

        List<Alternative> decisionAlternatives =
                alternatives.findByDecisionId(id);

        List<Criterion> decisionCriteria =
                criteria.findByDecisionId(id);

        for (Alternative alternative : decisionAlternatives) {
            scores.findByAlternativeId(alternative.getId())
                    .forEach(scores::delete);
        }

        for (Criterion criterion : decisionCriteria) {
            scores.findByCriterionId(criterion.getId())
                    .forEach(scores::delete);
        }

        alternatives.deleteAll(decisionAlternatives);
        criteria.deleteAll(decisionCriteria);
        decisions.delete(decision);
    }

    @PostMapping("/decisions/{id}/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addCriterion(
            @PathVariable Long id,
            @Valid @RequestBody CriterionRequest request,
            Authentication authentication) {

        Decision decision = findDecision(id, currentUser(authentication));

        Criterion criterion = criteria.save(
                new Criterion(
                        request.name().trim(),
                        request.weight(),
                        decision
                )
        );

        return criterionMap(criterion);
    }

    @DeleteMapping("/criteria/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCriterion(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser user = currentUser(authentication);

        Criterion criterion = criteria.findById(id)
                .orElseThrow(() -> notFound("Criterion"));

        // Verify that the criterion belongs to this user's decision.
        findDecision(criterion.getDecision().getId(), user);

        scores.findByCriterionId(id).forEach(scores::delete);
        criteria.delete(criterion);
    }

    @PostMapping("/decisions/{id}/alternatives")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addAlternative(
            @PathVariable Long id,
            @Valid @RequestBody AlternativeRequest request,
            Authentication authentication) {

        Decision decision = findDecision(id, currentUser(authentication));

        Alternative alternative = alternatives.save(
                new Alternative(
                        request.name().trim(),
                        request.description(),
                        decision
                )
        );

        return alternativeMap(alternative);
    }

    @DeleteMapping("/alternatives/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlternative(
            @PathVariable Long id,
            Authentication authentication) {

        AppUser user = currentUser(authentication);

        Alternative alternative = alternatives.findById(id)
                .orElseThrow(() -> notFound("Alternative"));

        // Verify ownership before deleting its scores or the alternative.
        findDecision(alternative.getDecision().getId(), user);

        scores.findByAlternativeId(id).forEach(scores::delete);
        alternatives.delete(alternative);
    }

    @PutMapping("/decisions/{decisionId}/scores")
    @Transactional
    public Map<String, Object> saveScore(
            @PathVariable Long decisionId,
            @Valid @RequestBody ScoreRequest request,
            Authentication authentication) {

        Decision decision = findDecision(
                decisionId,
                currentUser(authentication)
        );

        Alternative alternative = alternatives.findById(request.alternativeId())
                .orElseThrow(() -> notFound("Alternative"));

        Criterion criterion = criteria.findById(request.criterionId())
                .orElseThrow(() -> notFound("Criterion"));

        if (!alternative.getDecision().getId().equals(decision.getId())
                || !criterion.getDecision().getId().equals(decision.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Alternative and criterion must belong to this decision"
            );
        }

        Score score = scores.findByAlternativeIdAndCriterionId(
                        alternative.getId(),
                        criterion.getId())
                .orElseGet(() ->
                        new Score(alternative, criterion, request.score()));

        score.setScoreValue(request.score());
        Score saved = scores.save(score);

        return Map.of(
                "alternativeId", alternative.getId(),
                "criterionId", criterion.getId(),
                "score", saved.getScoreValue()
        );
    }

    @GetMapping("/decisions/{id}/results")
    public List<ResultRow> results(
            @PathVariable Long id,
            Authentication authentication) {

        Decision decision = findDecision(id, currentUser(authentication));

        List<Criterion> decisionCriteria =
                criteria.findByDecisionId(decision.getId());

        List<Alternative> decisionAlternatives =
                alternatives.findByDecisionId(decision.getId());

        List<Score> allScores =
                scores.findByAlternativeDecisionId(decision.getId());

        Map<String, Integer> scoreMap = allScores.stream()
                .collect(Collectors.toMap(
                        score -> score.getAlternative().getId()
                                + ":" + score.getCriterion().getId(),
                        Score::getScoreValue,
                        (first, second) -> second
                ));

        double totalWeight = decisionCriteria.stream()
                .mapToInt(Criterion::getWeight)
                .sum();

        List<ResultRow> rows = new ArrayList<>();

        for (Alternative alternative : decisionAlternatives) {
            double weighted = 0;

            if (totalWeight > 0) {
                for (Criterion criterion : decisionCriteria) {
                    Integer value = scoreMap.get(
                            alternative.getId() + ":" + criterion.getId()
                    );

                    if (value != null) {
                        weighted += value * criterion.getWeight();
                    }
                }
            }

            double normalized = totalWeight == 0
                    ? 0
                    : weighted / totalWeight;

            rows.add(new ResultRow(
                    alternative.getId(),
                    alternative.getName(),
                    Math.round(normalized * 100.0) / 100.0,
                    0
            ));
        }

        rows.sort(
                Comparator.comparingDouble(ResultRow::weightedScore)
                        .reversed()
                        .thenComparing(ResultRow::alternativeName)
        );

        List<ResultRow> ranked = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            ResultRow row = rows.get(i);

            ranked.add(new ResultRow(
                    row.alternativeId(),
                    row.alternativeName(),
                    row.weightedScore(),
                    i + 1
            ));
        }

        return ranked;
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in"
            );
        }

        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User account not found"
                ));
    }

    private Decision findDecision(Long id, AppUser user) {
        return decisions.findByIdAndOwner_Id(id, user.getId())
                .orElseThrow(() -> notFound("Decision"));
    }

    private ResponseStatusException notFound(String name) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                name + " not found"
        );
    }

    private Map<String, Object> summary(Decision decision) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", decision.getId());
        map.put("title", decision.getTitle());
        map.put("description", decision.getDescription());
        map.put("createdAt", decision.getCreatedAt());
        map.put(
                "criteriaCount",
                criteria.findByDecisionId(decision.getId()).size()
        );
        map.put(
                "alternativesCount",
                alternatives.findByDecisionId(decision.getId()).size()
        );

        return map;
    }

    private Map<String, Object> detail(Decision decision) {
        Map<String, Object> map = summary(decision);

        map.put(
                "criteria",
                criteria.findByDecisionId(decision.getId())
                        .stream()
                        .map(this::criterionMap)
                        .toList()
        );

        map.put(
                "alternatives",
                alternatives.findByDecisionId(decision.getId())
                        .stream()
                        .map(this::alternativeMap)
                        .toList()
        );

        map.put(
                "scores",
                scores.findByAlternativeDecisionId(decision.getId())
                        .stream()
                        .map(score -> Map.of(
                                "alternativeId",
                                score.getAlternative().getId(),
                                "criterionId",
                                score.getCriterion().getId(),
                                "score",
                                score.getScoreValue()
                        ))
                        .toList()
        );

        return map;
    }

    private Map<String, Object> criterionMap(Criterion criterion) {
        return Map.of(
                "id", criterion.getId(),
                "name", criterion.getName(),
                "weight", criterion.getWeight()
        );
    }

    private Map<String, Object> alternativeMap(Alternative alternative) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", alternative.getId());
        map.put("name", alternative.getName());
        map.put("description", alternative.getDescription());

        return map;
    }

    public record DecisionRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 1200) String description) {
    }

    public record CriterionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull @Min(1) @Max(10) Integer weight) {
    }

    public record AlternativeRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 800) String description) {
    }
}
