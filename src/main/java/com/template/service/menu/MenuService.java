package com.template.service.menu;

import java.util.List;

import com.template.dto.MenuRequest;
import com.template.dto.MenuResponse;
import com.template.dto.MenuTreeNode;
import com.template.dto.PageResult;
import com.template.entity.Menu;
import com.template.service.generic.GenericService;

public interface MenuService extends GenericService<Menu, Long>{

	List<MenuTreeNode> getMenuTreeForCurrentUser();
	PageResult<MenuResponse> findAll(String keyword, int page, int size);
	MenuResponse create(MenuRequest request);
	void update(MenuRequest request);
	void delete(Long id);
	MenuResponse getById(Long id);
	List<Menu> findAllMenus();
}
