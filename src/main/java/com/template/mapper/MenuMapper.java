package com.template.mapper;

import com.template.dto.MenuResponse;
import com.template.entity.Menu;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface MenuMapper extends Mapper<Menu> {

    List<Menu> findByRoleNames(@Param("roleNames") List<String> roleNames);

    List<MenuResponse> findAll(@Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    int countAll(@Param("keyword") String keyword);
}
