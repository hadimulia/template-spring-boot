package com.template.service.generic;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.entity.Condition;

@Slf4j
public abstract class GenericServiceImpl<T,ID extends Serializable> implements GenericService<T, ID>{

	private Class<T> entityClass;

	@SuppressWarnings("unchecked")
	public Class<T> getEntityClass() {
		if (entityClass == null) {
			entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
					.getActualTypeArguments()[0];
		}
		return entityClass;
	}
	
	protected Mapper<T> genericMapper;
	
	public GenericServiceImpl(Mapper<T> mapper) {
		this.genericMapper = mapper;
	}
	
	public List<T> getAll(){
		return genericMapper.selectAll();
	}
	
	public List<T> getByCriteria(Condition condition){
		return genericMapper.selectByExample(condition);
	}
	
	public T getOneByCriteria(Condition condition){
		return genericMapper.selectOneByExample(condition);
	}
	
	public T get(ID id) {
		return genericMapper.selectByPrimaryKey(id);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public T save(T entity) {
		if(ObjectUtils.isEmpty(getPrimaryValue(entity)))
			genericMapper.insertSelective(entity);
		else
			genericMapper.updateByPrimaryKeySelective(entity);
		return entity;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void remove(ID id) {
		genericMapper.deleteByPrimaryKey(id);
	}
	
	@SuppressWarnings("unchecked")
	private ID getPrimaryValue(T entity) {
		
		Class<T> clazz = getEntityClass();
		List<Field> fields = this.getAllFields(new LinkedList<>(), clazz);
		for (Field f : fields) {
			f.setAccessible(true);
			if (f.getAnnotation(javax.persistence.Id.class) == null )
				continue;
			try {
				return (ID) f.get(entity);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				log.error(e.getMessage(), e);
				return null;
			}
		}
		return null;
	}
	
	private List<Field> getAllFields(List<Field> fields, Class<?> type) {
		fields.addAll(Arrays.asList(type.getDeclaredFields()));

		if (type.getSuperclass() != null) {
			getAllFields(fields, type.getSuperclass());
		}

		return fields;
	}
}
