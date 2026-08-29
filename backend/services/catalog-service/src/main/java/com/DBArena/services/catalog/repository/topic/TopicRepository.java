package com.DBArena.services.catalog.repository.topic;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.services.catalog.domain.topic.Topic;

import java.util.Optional;

public interface TopicRepository {

    void insert(Topic topic);

    Optional<Topic> findBySlug(String slug);

    boolean existsBySlug(String slug);

    CursorPage<Topic> findPage(PageRequest pageRequest);
}
