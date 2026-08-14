package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {

    Optional<DailyLog> findByLogDate(LocalDate date);

    List<DailyLog> findAllByOrderByLogDateDesc();

    List<DailyLog> findByLogDateBetweenOrderByLogDate(LocalDate from, LocalDate to);
}
