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

import java.util.ArrayList;
import java.util.List;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.entityaop.DecodeUtil;
import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 支持树形结构的实体操作的DAO。
 */
public class TreeSupportDao<T extends IDecodable> extends BaseDao<T> implements ITreeSupportDao<T> {

    protected String entityName;
    
    public TreeSupportDao(Class<T> type) {
        super(type);
        entityName = type.getName();
    }

    /**
     * 读取同一层下节点的下一序号（当前最大序号 + 1）。
     * TODO 并发操作时，如何避免产生相同的seqNo？ 
     *     加唯一性约束【parentId, seqNo】 or 【decode】（都不行，排序等操作时候将会保存不了）
     * 
     * @param entityName
     * @param parentId
     * @return
     */
    public Integer getNextSeqNo(Long parentId) {
        return getNextSeqNo(entityName, parentId);
    }
    
    public Integer getNextSeqNo(String entityName, Long parentId) {
        String hql = "select max(o.seqNo) from " + entityName + " o where o.parentId = ?1";
        List<?> list = getEntities(hql, parentId); 
        return (!list.isEmpty() && list.get(0) != null) ? (Integer) list.get(0) + 1 : 1;
    }
    
    /**
     *  读取指定节点的所有子节点，不包括指定节点自身
     * @param entityName
     * @param decode
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getChildrenByDecode(String decode){
        String hql = "from " + entityName + " o where  o.decode <> ?1 and o.decode like ?2 order by o.decode";
        return (List<T>) getEntities(hql, decode, decode + "%");
    }
    
    /**
     * 读取指定节点的所有子节点，连同指定节点一并返回
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getChildrenById(Long id){
        return (List<T>) getChildrenById(entityName, id);
    }
    
    public List<?> getChildrenById(String entityName, Long id){
        String hql = "select o from " + entityName + " o, " + entityName + " o1 " +
        		" where o.decode like o1.decode||'%' and o1.id = ?1 order by o.decode";
        return getEntities(hql, id);
    }
    
    /**
     * 读取指定节点的所有子节点，不包含指定节点
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getChildrenExcludeSelfByDocode(String decode){
        String hql = "select o from " + entityName + " o " + 
                " where o.decode like ?1 and o.decode <> ?2 order by o.decode";
        return (List<T>)getEntities(hql, decode + "%", decode);
    }
    
    /**
     * 读取指定节点的所有父节点，连同指定节点一并返回
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getParentsById(Long id){
        return (List<T>)getParentsById(entityName, id);
    }
    
    public List<?> getParentsById(String entityName, Long id){
        String hql = "select o from " + entityName + " o, " + entityName + " o1 " +
        		" where o1.decode like o.decode||'%' and o1.id=?1 order by o.decode";
        return getEntities(hql, id);
    }
    
    /**
     * 读取指定节点（id）到截止节点（breakId）之间的所有父节点，连同指定节点和截止节点一并返回。
     * @param id
     * @param breakId 截止节点的ID
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getParentsById(Long id, Long breakId) {
        return (List<T>) getParentsById(entityName, id, breakId);
    }
    
    public List<?> getParentsById(String entityName, Long id, Long breakId) {
        IDecodable startnode = (IDecodable) getEntity(BeanUtil.createClassByName(entityName), id);
        IDecodable breaknode = (IDecodable) getEntity(BeanUtil.createClassByName(entityName), breakId);
        
        //如果资源表中找不到了该id对应的资源，则可能该资源已经被删除
        if(startnode == null) return new ArrayList<Object>();

        String hql = "from " + entityName + " o where ?1 like o.decode || '%' ";
        if (breaknode != null) {
            hql += " and decode like '" +breaknode.getDecode()+ "%' ";
        }
        hql += " order by decode";
        return getEntities(hql, startnode.getDecode());
    }
    
    /**
     * 读取排序时移动节点和目标节点之间的节点列表。
     * @param entityName
     * @param parentId
     * @param sourceOrder
     * @param targetOrder
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getRelationsNodeWhenSort(Long parentId, Integer sourceOrder, Integer targetOrder){
        Integer max = sourceOrder.compareTo(targetOrder) > 0 ? sourceOrder : targetOrder;
        Integer min = sourceOrder.compareTo(targetOrder) > 0 ? targetOrder : sourceOrder;
        String hql = " from " + entityName + " o where o.seqNo < ?1 and o.seqNo > ?2 and o.parentId = ?3";
        return (List<T>) getEntities(hql, max, min, parentId);
    }
    
    /**
     * 对同层节点进行排序。
     * @param id 排序节点
     * @param targetId  排序节点目标位置的节点
     * @param direction -1：向上移动  1：向下移动
     * @return
     */
    public List<T> sort(Long id, Long targetId, int direction) {
        T sourceItem = getEntity(id);
        T targetItem = getEntity(targetId);
        
        Long parentId = sourceItem.getParentId();
		if(!parentId.equals(targetItem.getParentId())) {
            throw new BusinessException(EX.F_02);
        }
        
        int sourceSeqNo = EasyUtils.obj2Int(sourceItem.getSeqNo());
        int targetSeqNo = EasyUtils.obj2Int(targetItem.getSeqNo());
        int tag = (sourceSeqNo < targetSeqNo ? 1 : -1);
    
        List<T> returnList = new ArrayList<T>();
        List<T> list = getRelationsNodeWhenSort(parentId, sourceSeqNo, targetSeqNo);
        for (T temp : list) {
            temp.setSeqNo(temp.getSeqNo() - tag);
            saveItem4Sort(temp, returnList);
        }
        sourceItem.setSeqNo(targetSeqNo + (direction - tag) / 2);
        targetItem.setSeqNo(targetSeqNo - (direction + tag) / 2);
        saveItem4Sort(sourceItem, returnList);
        saveItem4Sort(targetItem, returnList);
        
        /* 取出所有兄弟节点，按【1 --> size】的顺序重新进行排序设定，
         * 以消除存在相同序号的兄弟节点出现（相同序号则decode也相同，授权时打全勾会引起混乱，因为会把相同decode的兄弟节点及它的所有子节点都带上了）。
         */
        @SuppressWarnings("unchecked")
		List<T> sibling = (List<T>) getEntities("from " + entityName + " o where o.parentId = ? order by o.seqNo", parentId);
        int currentSeqNo = 1;
        for (T temp : sibling) {
        	if(currentSeqNo != EasyUtils.obj2Int(temp.getSeqNo())) {
        		temp.setSeqNo(currentSeqNo);
                saveItem4Sort(temp, returnList);
        	}
        	currentSeqNo ++;
        }
        
        return returnList; // 返回后
    }
    
    /** 保存排序中受影响的节点的所有子节点以维护decode值，并修复每个子节点下面用户的decode值信息 */
    void saveItem4Sort(T sortedNode, List<T> returnList) {
        String oldDecode = sortedNode.getDecode();
        
        saveDecodeableEntity(sortedNode, returnList);
        
        // 保存目标节点的子节点时，有可能把刚保存完的排序源节点给一起查出来了，需要过滤掉。
        List<T> children = getChildrenExcludeSelfByDocode(oldDecode);
        for (T temp : children) {
            saveDecodeableEntity(temp, returnList);
        }
    }
    
    /**
     * 保存树形结构的对象，并依据自身信息及父节点信息自动维护其decode值。
     * @param entity
     */
    public void saveDecodeableEntity(T entity) {   
        saveDecodeableEntity(entity, null);
    }
    
    private void saveDecodeableEntity(T entity, List<T> returnList) {   
        Integer levelNo = new Integer(1);
        int sectSize = DecodeUtil.getSectSize();
        
        //默认值，parentId==null || parentNode==null(像菜单)时取默认值
        String decode = DecodeUtil.getDecode(entity.getSeqNo(), sectSize); 
        
        //首先根据当前保存的节点找到其父节点, 然后根据其父节点的信息和本身的seqNo生成decode,levelNo
        Long parentId = entity.getParentId();
        if(parentId != null ) { // 找的到父节点的处理（资源试图中将 "全部" 节点id = 0注册进来）
            IEntity parentNode = getEntity(entity.getParentClass(), parentId);
            if(parentNode != null) {
            	String parentDecode;
            	if(parentNode instanceof IDecodable) {
            		IDecodable parent = (IDecodable) parentNode;
            		levelNo = (parent.getLevelNo() == null ? 0 : parent.getLevelNo()) + 1; // 层级加一
            		parentDecode = parent.getDecode();
            	} 
            	else { // parent可能是root（作为资源根节点，AbstractResourceView没有继承IDecode）
            		levelNo = 2; // 默认为第二级，root为第一级
            		parentDecode = (String) BeanUtil.getPropertyValue(parentNode, "decode"); // "00001"
            	}
                
                decode = DecodeUtil.getDecode(parentDecode, entity.getSeqNo(), sectSize);             
            }
        }
        entity.setDecode(decode);
        entity.setLevelNo(levelNo);
        
        if(entity.getId() == null) {  // ID为null，说明是新建
            createWithoutFlush(entity);
        } else {
            updateWithoutFlush(entity);
        }
        
        if( returnList != null && !returnList.contains(entity) ) {
            returnList.add(entity);
        }
    }
    
	public void moveEntity(T entity) {
	    update(entity);
	}
}
