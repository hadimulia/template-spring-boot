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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.template.dto.menu.MenuRequest;
import com.template.dto.menu.MenuResponse;
import com.template.dto.menu.MenuTreeNode;
import com.template.dto.PageResult;
import com.template.entity.menu.Menu;
import com.template.mapper.menu.MenuMapper;
import com.template.service.audit.Auditable;
import com.template.service.generic.GenericServiceImpl;
import com.template.util.MenuTreeBuilder;
import com.template.util.SecurityUtils;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MenuServiceImpl extends GenericServiceImpl<Menu, Long> implements MenuService{

    private final MenuMapper menuMapper;

    public MenuServiceImpl(MenuMapper mapper) {
        super(mapper);
        this.menuMapper = mapper;
    }

    private static final String SESSION_MENU_TREE_KEY = "menuTreeCache";

    @Transactional(readOnly = true)
    public List<MenuTreeNode> getMenuTreeForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) {
            return new ArrayList<>();
        }

        HttpSession session = getSession();
        if (session != null) {
            @SuppressWarnings("unchecked")
            List<MenuTreeNode> cached = (List<MenuTreeNode>) session.getAttribute(SESSION_MENU_TREE_KEY);
            if (cached != null) {
                return cached;
            }
        }

        List<MenuTreeNode> tree = buildMenuTree(auth);
        if (session != null && !tree.isEmpty()) {
            session.setAttribute(SESSION_MENU_TREE_KEY, tree);
        }
        return tree;
    }

    private boolean isAnonymous(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
    }

    private List<MenuTreeNode> buildMenuTree(Authentication auth) {
        Set<String> roleNames = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toSet());

        List<Menu> menus = menuMapper.findByRoleNames(new ArrayList<>(roleNames));
        return MenuTreeBuilder.buildTree(menus);
    }

    private void invalidateMenuTreeCache() {
        HttpSession session = getSession();
        if (session != null) {
            session.removeAttribute(SESSION_MENU_TREE_KEY);
        }
    }

    private static HttpSession getSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getSession(true);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public PageResult<MenuResponse> findAll(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<MenuResponse> data = menuMapper.findAll(keyword, offset, size);
        int total = menuMapper.countAll(keyword);
        return PageResult.of(data, total, page, size);
    }

    @Auditable(action = "CREATE", entityType = "MENU", description = "#request.name")
    @Transactional
    public MenuResponse create(MenuRequest request) {
        Menu menu = new Menu();
        menu.setParentId(request.getParentId());
        menu.setName(request.getName());
        menu.setUrl(request.getUrl());
        menu.setIcon(request.getIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setVisible(request.getVisible());
        menu.setI18nKey(request.getI18nKey());
        menu.setCreatedBy(SecurityUtils.getCurrentUsername());
        menu.setCreatedDate(LocalDateTime.now());
        menu.setDeleted(false);
        menu.setVersion(0);
        menu = save(menu);
        invalidateMenuTreeCache();
        return MenuResponse.of(menu);
    }

    @Auditable(action = "UPDATE", entityType = "MENU", description = "#request.name")
    @Transactional
    public void update(MenuRequest request) {
        Menu menu = get(request.getId());
        if (menu != null) {
            menu.setParentId(request.getParentId());
            menu.setName(request.getName());
            menu.setUrl(request.getUrl());
            menu.setIcon(request.getIcon());
            menu.setSortOrder(request.getSortOrder());
            menu.setVisible(request.getVisible());
            menu.setI18nKey(request.getI18nKey());
            menu.setUpdatedBy(SecurityUtils.getCurrentUsername());
            menu.setUpdatedDate(LocalDateTime.now());
            save(menu);
            invalidateMenuTreeCache();
        }
    }

    @Auditable(action = "DELETE", entityType = "MENU", description = "")
    @Transactional
    public void delete(Long id) {
        Menu menu = get(id);
        if (menu != null) {
            menu.setDeleted(true);
            menu.setUpdatedBy(SecurityUtils.getCurrentUsername());
            menu.setUpdatedDate(LocalDateTime.now());
            save(menu);
            invalidateMenuTreeCache();
        }
    }

    @Transactional(readOnly = true)
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
                .i18nKey(menu.getI18nKey())
                .createdBy(menu.getCreatedBy())
                .createdDate(menu.getCreatedDate())
                .updatedBy(menu.getUpdatedBy())
                .updatedDate(menu.getUpdatedDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Menu> findAllMenus() {
        return getAll().stream()
                .filter(m -> !Boolean.TRUE.equals(m.getDeleted()))
                .collect(Collectors.toList());
    }
}
