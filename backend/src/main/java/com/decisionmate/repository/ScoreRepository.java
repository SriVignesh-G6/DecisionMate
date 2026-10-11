package com.decisionmate.repository;
import com.decisionmate.model.Score;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findByAlternativeIdAndCriterionId(Long alternativeId, Long criterionId);
    List<Score> findByAlternativeDecisionId(Long decisionId);
    List<Score> findByCriterionId(Long criterionId);
    List<Score> findByAlternativeId(Long alternativeId);
}
