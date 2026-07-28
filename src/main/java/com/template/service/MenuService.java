package com.template.service;

import com.template.dto.MenuTreeNode;
import com.template.dto.MenuResponse;
import com.template.dto.MenuRequest;
import com.template.dto.PageResult;
import com.template.entity.Menu;
import com.template.entity.RoleMenu;
import com.template.mapper.MenuMapper;
import com.template.mapper.RoleMenuMapper;
import com.template.util.MenuTreeBuilder;
import com.template.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public List<MenuTreeNode> getMenuTreeForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ArrayList<>();
        }

        Set<String> roleNames = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());

        List<Menu> menus = menuMapper.findByRoleNames(new ArrayList<>(roleNames));
        return MenuTreeBuilder.buildTree(menus);
    }

    @Transactional(readOnly = true)
    public PageResult<MenuResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<MenuResponse> data = menuMapper.findAll(keyword, offset, size);
        int total = menuMapper.countAll(keyword);
        return PageResult.of(data, total, page, size);
    }

    public void create(MenuRequest request) {
        Menu menu = new Menu();
        menu.setParentId(request.getParentId());
        menu.setName(request.getName());
        menu.setUrl(request.getUrl());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setVisible(request.getVisible());
        menu.setCreatedBy(SecurityUtils.getCurrentUsername());
        menu.setCreatedDate(LocalDateTime.now());
        menu.setDeleted(false);
        menu.setVersion(0);
        menuMapper.insert(menu);
    }

    public void update(Long id, MenuRequest request) {
        Menu menu = menuMapper.selectByPrimaryKey(id);
        if (menu != null) {
            menu.setParentId(request.getParentId());
            menu.setName(request.getName());
            menu.setUrl(request.getUrl());
            menu.setIcon(request.getIcon());
            menu.setSortOrder(request.getSortOrder());
            menu.setVisible(request.getVisible());
            menu.setUpdatedBy(SecurityUtils.getCurrentUsername());
            menu.setUpdatedDate(LocalDateTime.now());
            menuMapper.updateByPrimaryKey(menu);
        }
    }

    public void delete(Long id) {
        Menu menu = menuMapper.selectByPrimaryKey(id);
        if (menu != null) {
            menu.setDeleted(true);
            menu.setUpdatedBy(SecurityUtils.getCurrentUsername());
            menu.setUpdatedDate(LocalDateTime.now());
            menuMapper.updateByPrimaryKey(menu);
        }
    }

    public MenuResponse getById(Long id) {
        Menu menu = menuMapper.selectByPrimaryKey(id);
        if (menu == null) return null;

        return MenuResponse.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .name(menu.getName())
                .url(menu.getUrl())
                .icon(menu.getIcon())
                .sortOrder(menu.getSortOrder())
                .visible(menu.getVisible())
                .createdBy(menu.getCreatedBy())
                .createdDate(menu.getCreatedDate())
                .updatedBy(menu.getUpdatedBy())
                .updatedDate(menu.getUpdatedDate())
                .build();
    }

    public List<Menu> findAllMenus() {
        return menuMapper.selectAll().stream()
                .filter(m -> !Boolean.TRUE.equals(m.getDeleted()))
                .collect(Collectors.toList());
    }
}
