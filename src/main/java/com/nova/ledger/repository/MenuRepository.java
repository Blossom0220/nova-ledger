package com.nova.ledger.repository;

import com.nova.ledger.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByDeletedFalseOrderBySortOrderAsc();

    List<Menu> findByParentIdAndDeletedFalseOrderBySortOrderAsc(Long parentId);

    List<Menu> findByTypeAndDeletedFalseOrderBySortOrderAsc(Menu.MenuType type);

    @Query("SELECT m FROM Menu m WHERE m.deleted = false AND m.hidden = false ORDER BY m.sortOrder ASC")
    List<Menu> findAllVisibleMenus();

    @Query("SELECT m FROM Menu m WHERE m.deleted = false AND m.parentId IS NULL ORDER BY m.sortOrder ASC")
    List<Menu> findRootMenus();

    @Query("SELECT COUNT(m) > 0 FROM Menu m WHERE m.parentId = :parentId AND m.deleted = false")
    boolean hasChildren(@Param("parentId") Long parentId);

    Optional<Menu> findByIdAndDeletedFalse(Long id);

    @Query("SELECT m FROM Menu m WHERE m.deleted = false AND m.permission = :permission")
    Optional<Menu> findByPermission(@Param("permission") String permission);

    @Query("SELECT m FROM Menu m WHERE m.deleted = false AND m.path = :path")
    Optional<Menu> findByPath(@Param("path") String path);
}
