package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Tag;
import com.nova.ledger.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> getTags(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(tagService.getUserTags(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tag>> createTag(@Valid @RequestBody Tag tag, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        tag.setUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("标签创建成功", tagService.createTag(tag)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Tag>> updateTag(
            @PathVariable Long id, @Valid @RequestBody Tag tag, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("标签更新成功", tagService.updateTag(id, userId, tag)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        tagService.deleteTag(id, userId);
        return ResponseEntity.ok(ApiResponse.success("标签删除成功", null));
    }
}
