package com.mhc.dashboard.repositories;

import com.mhc.dashboard.models.SyllabusCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyllabusCategoryRepository extends JpaRepository<SyllabusCategory, Long> {
    List<SyllabusCategory> findByPart(String part);
}
