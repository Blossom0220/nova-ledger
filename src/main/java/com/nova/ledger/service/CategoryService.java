package com.nova.ledger.service;

import com.nova.ledger.entity.Category;
import com.nova.ledger.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = "categoryTree", key = "#userId + ':' + #bookId + ':' + (#type != null ? #type.name() : 'ALL')")
    public List<CategoryVO> getCategoryTree(Long bookId, Long userId, Category.CategoryType type) {
        List<Category> allCategories;
        if (type != null) {
            allCategories = categoryRepository.findByBookIdAndTypeAndDeletedFalseOrderBySortOrderAsc(bookId, type);
        } else {
            allCategories = categoryRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId);
        }

        Map<Long, List<Category>> parentMap = allCategories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<CategoryVO> tree = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getParentId() == null) {
                tree.add(buildTree(category, parentMap));
            }
        }

        return tree;
    }

    private CategoryVO buildTree(Category category, Map<Long, List<Category>> parentMap) {
        CategoryVO vo = CategoryVO.from(category);
        List<Category> children = parentMap.getOrDefault(category.getId(), List.of());
        List<CategoryVO> childVOs = new ArrayList<>();
        for (Category child : children) {
            childVOs.add(buildTree(child, parentMap));
        }
        return new CategoryVO(vo.id(), vo.parentId(), vo.name(), vo.icon(),
                vo.color(), vo.type(), vo.sortOrder(), childVOs);
    }

    public Category getCategory(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        if (!category.getUserId().equals(userId) || category.getDeleted()) {
            throw new RuntimeException("无权访问该分类");
        }
        return category;
    }

    @Transactional
    @CacheEvict(value = "categoryTree", key = "#userId + ':' + #category.bookId")
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = "categoryTree", key = "#userId + ':' + #bookId")
    public Category updateCategory(Long id, Long bookId, Long userId, Category update) {
        Category category = getCategory(id, userId);
        category.setName(update.getName());
        category.setIcon(update.getIcon());
        category.setColor(update.getColor());
        category.setSortOrder(update.getSortOrder());
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = "categoryTree", key = "#userId + ':' + #bookId")
    public void deleteCategory(Long id, Long bookId, Long userId) {
        Category category = getCategory(id, userId);
        if (categoryRepository.existsByParentIdAndDeletedFalse(id)) {
            throw new RuntimeException("该分类下有子分类，请先删除子分类");
        }
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    public record CategoryVO(Long id, Long parentId, String name, String icon, String color,
                             Category.CategoryType type, Integer sortOrder, List<CategoryVO> children) {
        public static CategoryVO from(Category c) {
            return new CategoryVO(c.getId(), c.getParentId(), c.getName(), c.getIcon(),
                    c.getColor(), c.getType(), c.getSortOrder(), new ArrayList<>());
        }
    }
}
