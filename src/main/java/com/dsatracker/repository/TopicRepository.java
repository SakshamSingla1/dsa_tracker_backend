package com.dsatracker.repository;

import com.dsatracker.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findAllByOrderByOrderIndexAsc();
    List<Topic> findAllBySheetSlugOrderByOrderIndexAsc(String sheetSlug);
    List<Topic> findAllByOrderBySheet_OrderIndexAscOrderIndexAsc();
}
