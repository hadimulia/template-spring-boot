package com.template.service.generic;

import java.io.Serializable;
import java.util.List;
import tk.mybatis.mapper.entity.Condition;

public interface GenericService<T, ID extends Serializable> {
   List<T> getAll();

   List<T> getByCriteria(Condition condition);
   T getOneByCriteria(Condition condition);
   
   T get(ID id);

   T save(T entity);

   void remove(ID id);
}
