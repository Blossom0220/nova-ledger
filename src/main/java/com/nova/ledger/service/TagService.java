package com.nova.ledger.service;

import com.nova.ledger.entity.Tag;
import com.nova.ledger.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<Tag> getUserTags(Long userId) {
        return tagRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public Tag createTag(Tag tag) {
        if (tagRepository.existsByUserIdAndName(tag.getUserId(), tag.getName())) {
            throw new RuntimeException("标签名称已存在");
        }
        return tagRepository.save(tag);
    }

    @Transactional
    public Tag updateTag(Long id, Long userId, Tag update) {
        Tag tag = getTag(id, userId);
        tag.setName(update.getName());
        tag.setColor(update.getColor());
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long id, Long userId) {
        Tag tag = getTag(id, userId);
        tagRepository.delete(tag);
    }

    public Tag getTag(Long id, Long userId) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        if (!tag.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该标签");
        }
        return tag;
    }
}
