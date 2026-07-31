package com.template.service.generic;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.template.entity.tenant.Tenant;
import com.template.tenant.TenantContext;

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
		return genericMapper.selectByExample(buildScopedCondition());
	}

	public List<T> getByCriteria(Condition condition){
		applyTenantScope(condition);
		return genericMapper.selectByExample(condition);
	}

	public T getOneByCriteria(Condition condition){
		applyTenantScope(condition);
		return genericMapper.selectOneByExample(condition);
	}

	public T get(ID id) {
		if (!isTenantScoped()) {
			return genericMapper.selectByPrimaryKey(id);
		}
		Condition condition = new Condition(getEntityClass());
		condition.createCriteria().andEqualTo("id", id).andEqualTo("tenantId", TenantContext.getTenantId());
		return genericMapper.selectOneByExample(condition);
	}

	private boolean isTenantScoped() {
		if (getEntityClass() == null || Tenant.class.isAssignableFrom(getEntityClass())) {
			return false;
		}
		// Only scope entities that actually carry a tenant_id column.
		// Global reference entities (Role, Menu, Permission, junctions) must not be scoped.
		return hasTenantIdField();
	}

	private boolean hasTenantIdField() {
		for (Class<?> clazz = getEntityClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
			try {
				clazz.getDeclaredField("tenantId");
				return true;
			} catch (NoSuchFieldException ignored) {
				// continue
			}
		}
		return false;
	}

	private Condition buildScopedCondition() {
		Condition condition = new Condition(getEntityClass());
		if (isTenantScoped() && TenantContext.getTenantId() != null) {
			condition.createCriteria().andEqualTo("tenantId", TenantContext.getTenantId());
		}
		return condition;
	}

	private void applyTenantScope(Condition condition) {
		if (!isTenantScoped() || TenantContext.getTenantId() == null) {
			return;
		}
		if (condition.getOredCriteria().isEmpty()) {
			condition.createCriteria().andEqualTo("tenantId", TenantContext.getTenantId());
		} else {
			condition.and().andEqualTo("tenantId", TenantContext.getTenantId());
		}
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
