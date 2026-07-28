package com.template.service.menu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.template.dto.MenuRequest;
import com.template.dto.MenuResponse;
import com.template.dto.MenuTreeNode;
import com.template.dto.PageResult;
import com.template.entity.Menu;
import com.template.mapper.MenuMapper;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.MenuTreeBuilder;
import com.template.util.SecurityUtils;

@Service
@Transactional
public class MenuServiceImpl extends GenericServiceImpl<Menu, Long> implements MenuService{

	private MenuMapper menuMapper;
	
    public MenuServiceImpl(MenuMapper mapper) {
		super(mapper);
		this.menuMapper = mapper;
	}


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

    public MenuResponse create(MenuRequest request) {
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
        menu = save(menu);
        return MenuResponse.of(menu);
    }

    public void update(MenuRequest request) {
        Menu menu = get(request.getId());
        if (menu != null) {
            menu.setParentId(request.getParentId());
            menu.setName(request.getName());
            menu.setUrl(request.getUrl());
            menu.setIcon(request.getIcon());
            menu.setSortOrder(request.getSortOrder());
            menu.setVisible(request.getVisible());
            menu.setUpdatedBy(SecurityUtils.getCurrentUsername());
            menu.setUpdatedDate(LocalDateTime.now());
            save(menu);
        }
    }

    public void delete(Long id) {
        Menu menu = get(id);
        if (menu != null) {
            menu.setDeleted(true);
            menu.setUpdatedBy(SecurityUtils.getCurrentUsername());
            menu.setUpdatedDate(LocalDateTime.now());
            save(menu);
        }
    }

    public MenuResponse getById(Long id) {
        Menu menu = get(id);
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
        return getAll().stream()
                .filter(m -> !Boolean.TRUE.equals(m.getDeleted()))
                .collect(Collectors.toList());
    }
}
