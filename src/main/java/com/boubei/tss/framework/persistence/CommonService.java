/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("CommonService")
public class CommonService implements ICommonService {
	
	@Autowired private ICommonDao commonDao;

	public void create(IEntity entity) {
		commonDao.createObject(entity);
	}
	
	public void createWithLog(IEntity entity) {
		commonDao.createWithLog(entity);
	}
	
	public void update(IEntity entity) {
		commonDao.update(entity);
	}
	
	public void updateWithLog(IEntity entity) {
		commonDao.updateWithLog(entity);
	}

	public void delete(Class<?> entityClass, Long id) {
		commonDao.delete(entityClass, id);
	}

	public void deleteWithLog(Class<?> entityClass, Long id) {
		commonDao.deleteWithLog(entityClass, id);
	}

	public List<?> getList(String hql, Object...params) {
		return commonDao.getEntities(hql, params);
	}
	
	public List<?> getList(String hql, String[] args, Object[] params) {
		return commonDao.getEntities(hql, args, params);
	}
	
	public IEntity getEntity(Class<?> entityClass, Long id) {
		return commonDao.getEntity(entityClass, id);
	}

	public void createBatch(List<? extends IEntity> entitys) {
		for(IEntity entity : entitys) {
			create(entity);
		}
	}

	public void updateBatch(List<? extends IEntity> entitys) {
		for(IEntity entity : entitys) {
			update(entity);
		}
	}

	public void deleteBatch(Class<?> entityClass, List<Long> idList) {
		for(Long id : idList) {
			delete(entityClass, id);
		}
	}
}
