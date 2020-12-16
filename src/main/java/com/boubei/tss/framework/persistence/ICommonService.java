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

public interface ICommonService {

	void create(IEntity entity);
	void update(IEntity entity);
	void delete(Class<?> entityClass, Long id);
	
	void createBatch(List<? extends IEntity> entitys);
	void updateBatch(List<? extends IEntity> entitys);
	void deleteBatch(Class<?> entityClass, List<Long> idList);
	
	void createWithLog(IEntity entity);
	void updateWithLog(IEntity entity);
	void deleteWithLog(Class<?> entityClass, Long id);

	List<?> getList(String hql, Object...params);
	
	List<?> getList(String hql, String[] args, Object[] params);
	
	IEntity getEntity(Class<?> entityClass, Long id);

}
