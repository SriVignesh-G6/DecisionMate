package com.decisionmate.repository;
import com.decisionmate.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DecisionRepository extends JpaRepository<Decision, Long> {}
