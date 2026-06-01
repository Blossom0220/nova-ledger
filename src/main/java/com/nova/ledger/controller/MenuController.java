package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Menu;
import com.nova.ledger.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuService.MenuVO>>> getAllMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllMenus()));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuService.MenuVO>>> getMenuTree() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenuTree()));
    }

    @GetMapping("/visible")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuService.MenuVO>>> getVisibleMenus() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getVisibleMenus()));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuService.MenuVO>>> getMenusByType(
            @PathVariable Menu.MenuType type) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenusByType(type)));
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuService.MenuVO>>> getChildMenus(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getChildMenus(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MenuService.MenuVO>> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenu(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Menu>> createMenu(@Valid @RequestBody Menu menu) {
        return ResponseEntity.ok(ApiResponse.success("菜单创建成功", menuService.createMenu(menu)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Menu>> updateMenu(
            @PathVariable Long id,
            @Valid @RequestBody Menu menu) {
        return ResponseEntity.ok(ApiResponse.success("菜单更新成功", menuService.updateMenu(id, menu)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok(ApiResponse.success("菜单删除成功", null));
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> batchDeleteMenus(@RequestBody List<Long> ids) {
        menuService.batchDeleteMenus(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }
}
