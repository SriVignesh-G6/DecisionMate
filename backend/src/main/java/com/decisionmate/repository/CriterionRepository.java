package com.decisionmate.repository;
import com.decisionmate.model.Criterion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CriterionRepository extends JpaRepository<Criterion, Long> {
    List<Criterion> findByDecisionId(Long decisionId);
}
