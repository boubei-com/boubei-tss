/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.TreeSupportDao;
import com.boubei.tss.portal.dao.IPortalDao;
import com.boubei.tss.portal.entity.Structure;
 
@Repository("PortalDao")
public class PortalDao extends TreeSupportDao<Structure> implements IPortalDao {

    public PortalDao() {
		super(Structure.class);
	}
    
    public Structure saveStructure(Structure ps) {
    	if(ps.getId() == null) {
            return create(ps);
        } 
    	else {
//            update(ps);
            return ps; 
        }
    }
    
    public void deleteStructure(Structure ps) {
        super.delete(em.merge(ps));
    }

    public List<Structure> getParentsById(Long id, String operationId) {
        return getParentsById(id);
    }

    public List<Structure> getChildrenById(Long id, String operationId) {
        return getChildrenById(id);
    }

    public Object[] getPortalComponents(Long portalId, Long currentThemeId){
        String[] hqls = new String[3];        
        hqls[0] = "from Component o where o.id in (select distinct ti.decorator.id from Structure p, ThemeInfo ti where p.portalId=? and p.id=ti.id.structureId and ti.id.themeId = ? and p.type<>0 and p.disabled<>1) or o.isDefault = 1 ";
        hqls[1] = "from Component o where o.id in (select distinct ti.layout.id    from Structure p, ThemeInfo ti where p.portalId=? and p.id=ti.id.structureId and ti.id.themeId = ? and p.type<>3 and p.disabled<>1) or o.isDefault = 1 ";
        hqls[2] = "from Component o where o.id in (select distinct t.id from Structure p, Component t where p.definer.id=t.id and p.type=3 and p.portalId=? and p.disabled<>1)";
        
        Object[] returnVal = new Object[3];   
        returnVal[0] = getEntities(hqls[0], portalId, currentThemeId); 
        returnVal[1] = getEntities(hqls[1], portalId, currentThemeId); 
        returnVal[2] = getEntities(hqls[2], portalId); 
        return returnVal;
    }
    
    public List<?> getThemesByPortal(Long portalId) {
        return getEntities("from Theme o where o.portalId = ? order by o.id", portalId);
    }
}