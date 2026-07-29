package com.template.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.template.dto.menu.MenuTreeNode;
import com.template.entity.menu.Menu;

public class MenuTreeBuilder {

    public static List<MenuTreeNode> buildTree(List<Menu> flatMenus) {
        Map<Long, MenuTreeNode> nodeMap = new HashMap<>();
        List<MenuTreeNode> roots = new ArrayList<>();

        for (Menu menu : flatMenus) {
            if (menu.getDeleted() != null && menu.getDeleted()) continue;
            if (menu.getVisible() != null && !menu.getVisible()) continue;

            MenuTreeNode node = MenuTreeNode.builder()
                    .id(menu.getId())
                    .name(menu.getName())
                    .url(menu.getUrl())
                    .icon(menu.getIcon())
                    .sortOrder(menu.getSortOrder())
                    .parentId(menu.getParentId())
                    .children(new ArrayList<>())
                    .build();

            nodeMap.put(menu.getId(), node);
        }

        for (MenuTreeNode node : nodeMap.values()) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                MenuTreeNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        sortRecursive(roots);

        return roots;
    }

    private static void sortRecursive(List<MenuTreeNode> nodes) {
        nodes.sort(Comparator.comparing(MenuTreeNode::getSortOrder,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (MenuTreeNode node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortRecursive(node.getChildren());
            }
        }
    }
}
