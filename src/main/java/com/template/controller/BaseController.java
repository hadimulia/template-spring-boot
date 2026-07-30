package com.template.controller;

import com.template.dto.menu.MenuTreeNode;
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

    private void markActive(List<MenuTreeNode> nodes, String currentUri) {
        for (MenuTreeNode node : nodes) {
            boolean isActive = node.getUrl() != null && currentUri.startsWith(node.getUrl());
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                markActive(node.getChildren(), currentUri);
                isActive = isActive || node.getChildren().stream().anyMatch(MenuTreeNode::isActive);
            }
            node.setActive(isActive);
        }
    }
}
