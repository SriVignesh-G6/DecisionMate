
package com.decisionmate.repository;

import com.decisionmate.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionRepository extends JpaRepository<Decision, Long> {

    List<Decision> findAllByOwner_Id(Long ownerId);

    Optional<Decision> findByIdAndOwner_Id(Long id, Long ownerId);
}
