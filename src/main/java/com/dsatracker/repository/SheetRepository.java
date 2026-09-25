package com.dsatracker.repository;

import com.dsatracker.model.Sheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SheetRepository extends JpaRepository<Sheet, Long> {
    List<Sheet> findAllByOrderByOrderIndexAsc();
    Optional<Sheet> findBySlug(String slug);
}
