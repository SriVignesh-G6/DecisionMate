package com.decisionmate.repository;
import com.decisionmate.model.Alternative;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AlternativeRepository extends JpaRepository<Alternative, Long> {
    List<Alternative> findByDecisionId(Long decisionId);
}
