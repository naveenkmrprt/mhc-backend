package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.ExamRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRuleSetRepository extends JpaRepository<ExamRuleSet, Long> {

    Optional<ExamRuleSet> findByExamCycleAndActiveTrue(String examCycle);

}
