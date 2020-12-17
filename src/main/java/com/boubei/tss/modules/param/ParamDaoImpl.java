/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.param;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.TreeSupportDao;
import com.boubei.tss.util.EasyUtils;

@Repository("ParamDao")
public class ParamDaoImpl extends TreeSupportDao<Param> implements ParamDao {

	public ParamDaoImpl() {
        super(Param.class);
    }
	
	public List<?> getAllParam(boolean includeHidden) {
		if(includeHidden) {
			return getEntities("from Param p order by p.decode");
		}
		return getEntities("from Param p where p.hidden <> 1 order by p.decode");
	}
	
	public Param getParamByCode(String code) {
		String hql = "from Param p where p.code = ?1 and p.disabled <> 1 order by p.decode";
        List<?> list = getEntities(hql, EasyUtils.checkNull(code, "code is null"));
        return list.size() > 0 ? (Param) list.get(0) : null;
	}
	
	public List<?> getCanAddGroups(){
		String hql = "from Param p where p.type = ? and p.hidden <> 1 order by p.decode";
		return getEntities(hql, ParamConstants.GROUP_PARAM_TYPE);
	}
	
    /**
     *  读取指定节点的所有子节点，不包括指定节点自身
     */
    @SuppressWarnings("unchecked")
	public List<Param> getChildrenByDecode(String decode) {
    	String hql = "from Param o " +
    			" where  o.decode <> ?1 and o.decode like ?2 and o.disabled <> 1 order by o.decode";
        return (List<Param>) getEntities(hql, decode, decode + "%");
    }
}