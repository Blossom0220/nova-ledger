package com.nova.ledger.repository;

import com.nova.ledger.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByCreatedAtAsc(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
