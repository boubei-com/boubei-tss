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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@Service("ParamService")
public class ParamServiceImpl implements ParamService, ParamListener {

    @Autowired private ParamDao paramDao;
    
    /**
     * 删除或修改完成后，触发param监听器列表，以刷新缓存 及更新定时任务、新建缓存池等依赖param模块的功能
     */
    public void fireListener(Param param) {
        for(ParamListener listener : ParamManager.listeners) {
        	listener.afterChange(param);
        }
    }
    
	/* 
	 * 删除或修改完成后，刷新缓存，如果已经被缓存的话。 对于下拉或树形参数，新增参数项也需要刷新
	 */
    public void afterChange(Param param) {
    	if(param == null) return;
		
		String paramCode = null;
        if(ParamConstants.NORMAL_PARAM_TYPE.equals(param.getType())) {
        	paramCode = param.getCode();
        } 
        else if(ParamConstants.ITEM_PARAM_TYPE.equals(param.getType())) {
        	List<Param> parents = paramDao.getParentsById(param.getId()); 
        	for(Param temp: parents) {
        		if(ParamConstants.NORMAL_PARAM_TYPE.equals(temp.getType())) {
                	paramCode = temp.getCode();
                	break;
                }
        	}
        }
        
        if(paramCode != null) {
        	Pool dataCache = JCache.getInstance().getPool(CacheLife.LONG.toString());
            Set<Object> keys = dataCache.listKeys();
            for(Object key : keys) {
            	String _key = key.toString();
            	if(_key.indexOf(".ParamService.") > 0 && _key.toLowerCase().indexOf(paramCode.toLowerCase()) >= 0) {
            		dataCache.destroyByKey(key);
            	}
            }
        }
    }
    
    /**
     * 只有管理员 或 创建者本人，才能执行删除、停用、修改等操作
     */
    private void checkPermission(Long paramId) {
    	List<Long> permitedUsers = new ArrayList<Long>();
    	permitedUsers.add(UMConstants.ADMIN_USER_ID); 
    	
    	paramId = EasyUtils.obj2Long(paramId);
    	Param param = paramDao.getEntity(paramId);
    	param = (Param) EasyUtils.checkNull(param, new Param());
    	
		permitedUsers.add(param.getCreatorId()); 
		if( !permitedUsers.contains( Environment.getUserId() ) ) {
    		throw new BusinessException(EX.F_12);
    	}
    }
 
    public void delete(Long id) {
    	checkPermission(id);
    	
        // 一并删除子节点
        List<?> children = paramDao.getChildrenById(id);
        for(Object entity : children) {
            Param item = (Param)entity;
            if(id.equals(item.getId())) {
            	item.setDisabled(ParamConstants.TRUE); // 先停用
            	fireListener(item); /* 在执行delete前触发触发器，否则执行如getParentsById等查询时，因o1已经删除，将查询不到数据 */
            	item = paramDao.getEntity(id);
            }
            paramDao.delete(item);
        }
    }
    
    public Param saveParam(Param param) {
    	Long id = param.getId();
        if (null == id) {
            param.setSeqNo(paramDao.getNextSeqNo(param.getParentId()));
            judgeLegit(param);
            paramDao.create(param);
        }
        else {
        	checkPermission(id);
        	
            judgeLegit(param);
            if(param.getLockVersion() == 0) { // 非param.htm维护系统参数的情况
            	Param old = paramDao.getEntity(id);
            	param.setLockVersion(old.getLockVersion());
            	param.setCreateTime(old.getCreateTime());
            	param.setCreatorId(old.getCreatorId());
            	param.setCreatorName(old.getCreatorName());
            }
            paramDao.update(param);
        }
        
        fireListener(param);

        return param;
    }
    
    public void saveParams(List<Map<String, Object>> list){
		for(Map<String, Object> map : list){
			Param param = new Param();
			BeanUtil.setDataToBean(param, map);	
			saveParam(param);
		}
	}
    
    /**
     * 字段重复判断。 （区分参数组、参数、参数项的概念） 不同参数的code不可以相同，确保每个参数的code值对整个参数表中的“参数”唯一
     */
    private void judgeLegit(Param param) {
        // 如果保存的是参数（区分参数组、参数、参数项的概念），则要保证code值对所有“参数”唯一
        Integer type = param.getType();
        if (ParamConstants.NORMAL_PARAM_TYPE.equals(type)) {
            String hql = "select p.id from Param p where p.type = ?1 and p.code = ?2";
            String code = param.getCode();
			List<?> list = paramDao.getEntities(hql, ParamConstants.NORMAL_PARAM_TYPE, code);
            list.remove(param.getId()); // 自己不算
            if (list.size() > 0) {
                throw new BusinessException(EX.parse(EX.F_13, code));
            }
            return;
        }

        String uniqueField;
        String hql = "select p.id from Param p where p.parentId=" + param.getParentId();
        if (ParamConstants.GROUP_PARAM_TYPE.equals(type)) { // 参数组不能同名
        	uniqueField = param.getName();
            hql += " and p.name='" + uniqueField + "'";
        } else {
            Param parentParam = paramDao.getEntity(param.getParentId());
            param.setModality(parentParam.getModality());
            
            uniqueField = param.getText();
            hql += " and p.text='" + uniqueField + "' ";
        }

        List<?> list = paramDao.getEntities(hql);
        list.remove(param.getId()); // 自己不算
		if (list.size() > 0) {
            throw new BusinessException(EX.parse(EX.F_14, uniqueField));
        }
    }

    public void startOrStop(Long paramId, Integer disabled) {
    	checkPermission(paramId);
    	
        List<?> datas = ParamConstants.TRUE.equals(disabled) ? paramDao.getChildrenById(paramId) : paramDao.getParentsById(paramId);
        for (int i = 0; i < datas.size(); i++) {
            Param param = (Param) datas.get(i);
            param.setDisabled(disabled);
            paramDao.updateWithoutFlush(param);
            
            fireListener(param);
        }
        paramDao.flush();
    }

    public List<?> getAllParams(boolean includeHidden) {
        return paramDao.getAllParam(includeHidden);
    }

    public Param getParam(Long id) {
        Param param = paramDao.getEntity(id);
        if( param != null ) {
        	paramDao.evict(param);
        }
        return param;
    }

    public void sortParam(Long paramId, Long toParamId, int direction) {
        paramDao.sort(paramId, toParamId, direction);
    }

    public List<?> copyParam(Long paramId, Long toParamId) {
        Param copyParam = paramDao.getEntity(paramId);
        Param toParam   = paramDao.getEntity(toParamId);
        
        Long copyParamId = copyParam.getId();
        List<?> params = paramDao.getChildrenById(copyParamId);
        Map<Long, Long> paramMapping = new HashMap<Long, Long>(); // 复制出来的新节点 与 被复制源节点 建立一一对应关系（ID 对 ID）
        for (int i = 0; i < params.size(); i++) {
            Param param = (Param) params.get(i);
            Long sourceParamId = param.getId();
            
            paramDao.evict(param);
            param.setId(null);
            if (sourceParamId.compareTo(copyParamId) == 0) {
                param.setParentId(toParam.getId());
                param.setSeqNo(paramDao.getNextSeqNo(toParam.getId()));
            }
            else {
                param.setParentId(paramMapping.get(param.getParentId()));
            }
            
            // 如果目标根节点是停用状态，则所有新复制出来的节点也一律为停用状态
            param.setDisabled(toParam.getDisabled()); 
            param.setName( param.getName() + "_copy" );
            param.setCode( param.getCode() + "_copy" );
            judgeLegit(param);
            
            paramDao.create(param);
            paramMapping.put(sourceParamId, param.getId());
        }
        return params;
    }

    public void move(Long paramId, Long toParamId) {
    	checkPermission(paramId);
    	
        List<?> params = paramDao.getChildrenById(paramId);
        for (int i = 0; i < params.size(); i++) {
            Param param = (Param) params.get(i);
            if (param.getId().equals(paramId)) { // 判断是否是移动节点（即被移动枝的根节点）
                param.setSeqNo(paramDao.getNextSeqNo(toParamId));
                param.setParentId(toParamId);
            }
            
            // 如果目标根节点是停用状态，则所有移动过来的节点也一律为停用状态
            if(toParamId.longValue() > 0) { // 非_root
            	Param toParam = paramDao.getEntity(toParamId);
            	param.setDisabled(toParam.getDisabled()); 
            }
            
            paramDao.update(param);
        }
    }

    public List<?> getCanAddGroups() {
        return paramDao.getCanAddGroups();
    }
    
    public List<Param> getParamsByParentCode(String code) {
        Param parent = paramDao.getParamByCode(code);
        if (parent == null) {
        	return null;
        }
        return paramDao.getChildrenByDecode(parent.getDecode());
    }

    /* ************************  以下供ParamManager调用(不适合Param CRUD相关模块调用，因为配置了Cache) ************************** */
    
    // TODO 防止连接池配置等泄露出去
    public Param getParam(String code) {
    	return paramDao.getParamByCode(code);
    }

    public List<Param> getComboParam(String code) {
        Param param = paramDao.getParamByCode(code);
        if (param == null) {
        	return null;
        }
        if (!ParamConstants.COMBO_PARAM_MODE.equals(param.getModality())) {
            throw new BusinessException(code + " is not a combo param");
        }
        return paramDao.getChildrenByDecode(param.getDecode());
    }

    public List<Param> getTreeParam(String code) {
        Param param = paramDao.getParamByCode(code);
        if (param == null) {
        	return null;
        }
        if (!ParamConstants.TREE_PARAM_MODE.equals(param.getModality())) {
            throw new BusinessException(code + " is not a tree param");
        }
        return paramDao.getChildrenByDecode(param.getDecode());
    }
}
