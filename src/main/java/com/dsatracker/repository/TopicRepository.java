package com.dsatracker.repository;

import com.dsatracker.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findAllByOrderByOrderIndexAsc();
    List<Topic> findAllBySheetSlugOrderByOrderIndexAsc(String sheetSlug);
    List<Topic> findAllByOrderBySheet_OrderIndexAscOrderIndexAsc();

    /** Guards admin sheet deletion -- a sheet with topics under it can't be deleted without
     *  orphaning/cascading them, so the admin must remove its topics first. */
    boolean existsBySheetId(Long sheetId);

    List<Topic> findAllBySheetIdOrderByOrderIndexAsc(Long sheetId);
}
