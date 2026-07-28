package com.template.controller;

import com.template.dto.MenuTreeNode;
import com.template.service.menu.MenuServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class BaseController {

    private final MenuServiceImpl menuService;

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("menuTree")
    public List<MenuTreeNode> loadMenuTree(HttpServletRequest request) {
        List<MenuTreeNode> tree = menuService.getMenuTreeForCurrentUser();
        String currentUri = request.getRequestURI();
        markActive(tree, currentUri);
        return tree;
    }

    private boolean markActive(List<MenuTreeNode> nodes, String currentUri) {
        boolean anyActive = false;
        for (MenuTreeNode node : nodes) {
            // Leaf menu: active if URL matches current URI
            if (node.getUrl() != null && currentUri.startsWith(node.getUrl())) {
                node.setActive(true);
                anyActive = true;
            }
            // Parent menu: active if any child is active
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                boolean childActive = markActive(node.getChildren(), currentUri);
                if (childActive) {
                    node.setActive(true);
                    anyActive = true;
                }
            }
        }
        return anyActive;
    }
}
