package com.nova.ledger.service;

import com.nova.ledger.entity.Menu;
import com.nova.ledger.repository.MenuRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Data
    @Builder
    public static class MenuVO {
        private Long id;
        private Long parentId;
        private String name;
        private String title;
        private String path;
        private String component;
        private String icon;
        private String color;
        private String type;
        private String permission;
        private Integer sortOrder;
        private Boolean hidden;
        private Boolean keepAlive;
        private Boolean hasChildren;
        private List<MenuVO> children;
    }

    public List<MenuVO> getAllMenus() {
        List<Menu> menus = menuRepository.findByDeletedFalseOrderBySortOrderAsc();
        return buildMenuTree(menus);
    }

    public List<MenuVO> getMenuTree() {
        List<Menu> rootMenus = menuRepository.findRootMenus();
        return rootMenus.stream()
                .map(this::toVOWithChildren)
                .collect(Collectors.toList());
    }

    public List<MenuVO> getVisibleMenus() {
        List<Menu> menus = menuRepository.findAllVisibleMenus();
        return buildMenuTree(menus);
    }

    public List<MenuVO> getMenusByType(Menu.MenuType type) {
        List<Menu> menus = menuRepository.findByTypeAndDeletedFalseOrderBySortOrderAsc(type);
        return menus.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public List<MenuVO> getChildMenus(Long parentId) {
        List<Menu> menus = menuRepository.findByParentIdAndDeletedFalseOrderBySortOrderAsc(parentId);
        return menus.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public MenuVO getMenu(Long id) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("菜单不存在"));
        return toVOWithChildren(menu);
    }

    @Transactional
    public Menu createMenu(Menu menu) {
        validateMenu(menu);
        return menuRepository.save(menu);
    }

    @Transactional
    public Menu updateMenu(Long id, Menu update) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("菜单不存在"));

        if (update.getParentId() != null && update.getParentId().equals(id)) {
            throw new RuntimeException("不能将自己设为父菜单");
        }

        menu.setParentId(update.getParentId());
        menu.setName(update.getName());
        menu.setTitle(update.getTitle());
        menu.setPath(update.getPath());
        menu.setComponent(update.getComponent());
        menu.setIcon(update.getIcon());
        menu.setColor(update.getColor());
        menu.setType(update.getType());
        menu.setPermission(update.getPermission());
        menu.setSortOrder(update.getSortOrder());
        menu.setHidden(update.getHidden());
        menu.setKeepAlive(update.getKeepAlive());

        validateMenu(menu);
        return menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = menuRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("菜单不存在"));

        if (menuRepository.hasChildren(id)) {
            throw new RuntimeException("该菜单下有子菜单，无法删除");
        }

        menu.setDeleted(true);
        menuRepository.save(menu);
    }

    @Transactional
    public void batchDeleteMenus(List<Long> ids) {
        for (Long id : ids) {
            deleteMenu(id);
        }
    }

    private void validateMenu(Menu menu) {
        if (menu.getParentId() != null) {
            Menu parent = menuRepository.findByIdAndDeletedFalse(menu.getParentId())
                    .orElseThrow(() -> new RuntimeException("父菜单不存在"));

            if (parent.getType() == Menu.MenuType.BUTTON) {
                throw new RuntimeException("按钮不能作为父菜单");
            }
        }

        if (menu.getPath() != null && !menu.getPath().isEmpty()) {
            menuRepository.findByPath(menu.getPath())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(menu.getId())) {
                            throw new RuntimeException("菜单路径已存在");
                        }
                    });
        }
    }

    private List<MenuVO> buildMenuTree(List<Menu> menus) {
        Map<Long, List<Menu>> parentMap = menus.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getParentId() != null ? m.getParentId() : 0L,
                        Collectors.toList()
                ));

        List<Menu> rootMenus = parentMap.getOrDefault(0L, new ArrayList<>());
        return rootMenus.stream()
                .map(menu -> buildMenuTreeRecursive(menu, parentMap))
                .collect(Collectors.toList());
    }

    private MenuVO buildMenuTreeRecursive(Menu menu, Map<Long, List<Menu>> parentMap) {
        MenuVO vo = toVO(menu);
        List<Menu> children = parentMap.get(menu.getId());
        if (children != null && !children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(child -> buildMenuTreeRecursive(child, parentMap))
                    .collect(Collectors.toList()));
            vo.setHasChildren(true);
        }
        return vo;
    }

    private MenuVO toVO(Menu menu) {
        return MenuVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .name(menu.getName())
                .title(menu.getTitle())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .color(menu.getColor())
                .type(menu.getType().name())
                .permission(menu.getPermission())
                .sortOrder(menu.getSortOrder())
                .hidden(menu.getHidden())
                .keepAlive(menu.getKeepAlive())
                .hasChildren(menuRepository.hasChildren(menu.getId()))
                .build();
    }

    private MenuVO toVOWithChildren(Menu menu) {
        MenuVO vo = toVO(menu);
        List<MenuVO> children = getChildMenus(menu.getId());
        if (!children.isEmpty()) {
            vo.setChildren(children);
        }
        return vo;
    }
}
