package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Category;
import com.nova.ledger.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ledger/books/{bookId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryService.CategoryVO>>> getCategories(
            @PathVariable Long bookId,
            @RequestParam(required = false) Category.CategoryType type,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.getCategoryTree(bookId, userId, type)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @PathVariable Long bookId,
            @Valid @RequestBody Category category,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        category.setUserId(userId);
        category.setBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("分类创建成功",
                categoryService.createCategory(category)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long bookId,
            @PathVariable Long id,
            @Valid @RequestBody Category category,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("分类更新成功",
                categoryService.updateCategory(id, bookId, userId, category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long bookId,
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        categoryService.deleteCategory(id, bookId, userId);
        return ResponseEntity.ok(ApiResponse.success("分类删除成功", null));
    }
}
