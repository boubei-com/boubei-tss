/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boubei.tss.PX;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.persistence.TreeSupportDao;
import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.tree.ILevelTreeNode;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.permission.dispaly.ResourceTreeNode;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 操作资源表时相关的权限补齐操作和删除操作。
 * 
 * PermissionHelper 配置到各个应用中，负责处理基于TSS框架的应用里的资源表、权限表的操作。
 * 针对TSS特有的操作不宜放在本接口里，比如读取角色信息等。
 */
@Component("permissionHelper")
public class PermissionHelper extends TreeSupportDao<IDecodable> {
    
    public PermissionHelper() {
		super(IDecodable.class);
	}
    
    @Autowired private RemoteResourceTypeDao resourceTypeDao;
    
    
    public static PermissionHelper getInstance(){
        return (PermissionHelper) Global.getBean("permissionHelper");
    }
    
    /** 获取当前应用ID */
    public static String getApplicationID() {
        return Config.getAttribute(PX.APPLICATION_CODE).toLowerCase();
    }
    
    /**
     * 获取当前用户对点击资源节点（以及父节点）的操作权限
     * @param resourceId
     * @param permissionTable
     * @param resourceClass
     * @return
     * 
     * select distinct p.* from bd.dm_permission_record p, bd.um_roleusermapping ru 
		where p.resourceId = 108 
		  and p.roleId = ru.roleId 
		  and ru.roleId in (-10000, 2)
		  and ru.userId = 250
     */
    public List<String> getOperationsByResource(Long resourceId, String permissionTable, Class<?> resourceClass) {
        Long userId = Environment.getUserId();
        return getOperationsByResource(resourceId, permissionTable, resourceClass, userId);
    }
    
    public List<String> getOperationsByResource(Long resourceId, String permissionTable, Class<?> resourceClass, Long userId) {
    	List<String> operations = new ArrayList<String>();
        String hql = "select distinct p.operationId from " + permissionTable + " p, RoleUserMapping ru "  +
                " where p.resourceId = ?1 and p.roleId = ru.id.roleId and ru.id.userId = ?2 ";  
        
        userId = (Long) EasyUtils.checkNull(userId, Anonymous._ID);
		List<?> operationsOnResource = getEntities(hql, resourceId, userId);
        for(Object oprationId : operationsOnResource){
            operations.add( (String) oprationId );  // 用户对指定节点的操作权限
        }
        
        ILevelTreeNode resource = (ILevelTreeNode) getEntity(resourceClass, resourceId);
        List<?> parentOperations = getEntities(hql, resource.getParentId(), userId); 
        for(Object oprationId : parentOperations){
            operations.add("p_" + oprationId);  // 指定节点的父节点的操作权限，加"p_"
        }
        
        return operations;
    }
    
    /**
     * 判断某个角色对指定的资源节点是否拥有某项权限
     */
    public List<?> getOperationsByResourceAndRole(String permissionTable, Long resourceId, Long roleId) {
        String hql = "select distinct p.operationId from " + permissionTable + " p  where p.resourceId = ?1 and p.roleId = ?2 ";  
		return getEntities(hql, resourceId, roleId);
    }
    
    /**
     * 获取用户对一个应用中的一种资源类型中的一个资源拥有的权限选项ID集合
     * @param resourceTypeId
     * @param resourceId
     * @return
     */
    public List<?> getOperationsByResource(String resourceTypeId, Long resourceId) {
        String applicationId = PermissionHelper.getApplicationID();
		String permissionTable = resourceTypeDao.getPermissionTable(applicationId, resourceTypeId);
        String resourceTable = resourceTypeDao.getResourceTable(applicationId, resourceTypeId);
        
        Class<?> resourceClass = BeanUtil.createClassByName(resourceTable);
        
        return getOperationsByResource(resourceId, permissionTable, resourceClass);
    }
    
    /**
     * 创建一条授权信息（未补齐）
     * @param roleId
     * @param resourceId
     * @param operationId
     * @param permissionState
     * @param isGrant
     * @param isPass
     * @param permissionTable
     */
    public AbstractPermission createUnPermission(Long roleId, Long resourceId, String operationId, 
            Integer permissionState, Integer isGrant, Integer isPass, String permissionTable) {
        
    	AbstractPermission usPermission = (AbstractPermission) BeanUtil.newInstanceByName(permissionTable);         
        usPermission.setRoleId(roleId);
        usPermission.setResourceId(resourceId);
        usPermission.setOperationId(operationId);
        usPermission.setPermissionState(permissionState);
        usPermission.setIsGrant(isGrant);
        usPermission.setIsPass(isPass);
        
        return usPermission;
    }

    /**
     * 根据已有授权信息，新增一条授权信息。
     * @param usPermission 
     * 				节点的未补齐授权信息 或 父（祖）节点授权信息
     * @param resource 
     * 				节点资源
     * @param permissionTable
     * 
     * TODO 保存权限前要在当前授权级别的上一级去查找有没有已经授过的，如果授过的，则删掉。
     *      保存当前授权级别(一个资源对同一个权限选项只能有一种授权级别)
     */
    public void createPermission(AbstractPermission usPermission, IResource resource, String permissionTable){
        String operationId = usPermission.getOperationId(); 
        Long roleId = usPermission.getRoleId();
        Integer permissionState = usPermission.getPermissionState();
        Integer isGrant = usPermission.getIsGrant(); 
        Integer isPass = usPermission.getIsPass();

        createPermission(roleId, resource, operationId, permissionState, isGrant, isPass, permissionTable);
    }
    
    public void createPermission(Long roleId, IResource resource, String operationId, 
            Integer permissionState, Integer isGrant, Integer isPass, String permissionTable) {
    	
    	Class<?> permissionTableClass = BeanUtil.createClassByName(permissionTable);
    	AbstractPermission newPermission = (AbstractPermission) BeanUtil.newInstance(permissionTableClass);   
        newPermission.setOperationId(operationId);
        newPermission.setRoleId(roleId);
        newPermission.setPermissionState(permissionState);
        newPermission.setIsGrant(isGrant);
        newPermission.setIsPass(isPass);
        newPermission.setResourceId(resource.getId());
        newPermission.setResourceName(resource.getName());
        
        createObjectWithoutFlush(newPermission);
    }
    
    // ----------------------------------------------------- HQL builder -------------------------------------------------------------
    
    public static final String ORDER_BY = " order by o.decode ";

    public static String formatHQLFrom(String entityName, String permissionTable) {
        return " from " + entityName + " o, RoleUserMapping ru, " + permissionTable + " p ";
    }

    public static String permissionCondition() {
        return " where ru.id.roleId = p.roleId and ru.id.userId = ?1 and o.id = p.resourceId and p.operationId = ?2 ";
    }

    public static String permissionConditionII() {
        return " where ru.id.roleId = p.roleId and ru.id.userId = :operatorId and o.id = p.resourceId and p.operationId = :operationId ";
    }
    
    /**
     * 生成的HQL如下：
     * select distinct o from entityName o, RoleUserMapping ru, permissionTable p
     * where ru.id.roleId = p.roleId and ru.id.userId = ? and o.id = p.resourceId and p.operationId = ?
     * order by o.decode
     */
    public static String permissionHQL(String entityName, String permissionTable) {
        return permissionHQL(entityName, permissionTable, true);
    }
    
    public static String permissionHQL(String entityName, String permissionTable, boolean isLevelTree) {
        return permissionHQL(entityName, permissionTable, "", isLevelTree);
    }

    public static String permissionHQL(String entityName, String permissionTable, String conditon, boolean isLevelTree) {
        String hql = "select distinct o " + formatHQLFrom(entityName, permissionTable) + permissionCondition() + conditon;
        if(isLevelTree) {
            hql += ORDER_BY;
        }
        return hql;
    }
    
    // ----------------------------------------------------- 原 PermissDao -------------------------------------------------------------
    /**
     * PermissionDao配置到各个应用中。
     * 负责处理基于TSS框架的应用里的资源表、权限表（包括补齐和未补齐）的操作。
     * 针对TSS特有的操作不宜放在本接口里，比如读取角色信息等。
     */
    
    /**
     * <p>
     * 批量删除指定角色拥有的资源授权信息。【给角色授权】时使用。
     * 
     * 删除只删除本级别的，其他级别已经授权过的不要改变 。
     * </p>
	 * @param roleId
	 * @param permissionRank
	 * @param permissionTable
	 * @param resourceTable
	 */
	public void deletePermissionByRole(Long roleId, String permissionRank, String permissionTable, String resourceTable) {
        String rankCondition = genRankCondition4DeleletePermission(permissionRank);
        List<?> exsitPermissions = getEntities(" from " + permissionTable + " p where p.roleId = ?1 " + rankCondition, roleId);
        deleteAll(exsitPermissions);
	}
	
    /**
     * <p>
     * 批量删除指定资源相关授权信息。【授与角色】使用。
     * 
	 * 删除只删除本级别的,其他级别已经授权过的不变
     * </p>
	 * @param resourceId
	 * @param permissionRank
	 * @param permissionTable
	 * @param resourceTable
	 */
	public void deletePermissionByResource(Long resourceId, String permissionRank, String permissionTable, String resourceTable) {
        String rankCondition = genRankCondition4DeleletePermission(permissionRank);
        List<?> exsitPermissions = getEntities(" from " + permissionTable + " p where p.resourceId = ?1 " + rankCondition, resourceId);
    	deleteAll(exsitPermissions); 
    	
    	// 根据这些记录的PermissionState值，来判断在删除补全表时是否删除子节点的权限
    	for ( Object temp : exsitPermissions ) {
        	AbstractPermission permission = (AbstractPermission) temp;
            Long roleId = permission.getRoleId();
            String operationId = permission.getOperationId();
            
            // 如果授权状态是2(即PERMIT_SUB_TREE), 说明【其所有子节点的权限都】由该节点自动产生，需删除权限表中【其所有子节点】对应【该角色】【该权限项】的授权信息
            if (permission.getPermissionState().equals(UMConstants.PERMIT_SUB_TREE)) { 
                List<?> childResources = getChildrenById(resourceTable, resourceId);
                for (Object child : childResources) {
                    String hql = "delete " + permissionTable + " p where" + " p.resourceId=?1 and p.roleId=?2 and p.operationId=?3 ";
                    executeHQL(hql + rankCondition, ((IResource) child).getId(), roleId, operationId);
                }
            } 
        }
        
        flush();
    }
 
    String genRankCondition4DeleletePermission(String permissionRank){
    	if( UMConstants.IGNORE_PERMISSION.equals(permissionRank) ) {
    		return "";
    	}
        Integer[] isGrantAndPass = convertRank(permissionRank);
        return " and p.isGrant = " + isGrantAndPass[0] + " and p.isPass = " + isGrantAndPass[1];
    }
    
	/**
	 * 将授权级别转化为两查询条件isGrant 和 isPass 的值
	 * @param permissionRank
	 * @return
	 */
	Integer[] convertRank(String permissionRank) {
        Integer isGrant, isPass;
        if (UMConstants.AUTHORISE_PERMISSION.equals(permissionRank)) { // 可授权
            isGrant = ParamConstants.TRUE;
            isPass  = ParamConstants.FALSE;
        } 
        else if (UMConstants.PASSON_AUTHORISE_PERMISSION.equals(permissionRank)) { // 可授权可传递
            isGrant = isPass = ParamConstants.TRUE;
        }
        else { // 默认：UMConstants.LOWER_PERMISSION，普通授权
        	isGrant = isPass = ParamConstants.FALSE;
        }
        return new Integer[] {isGrant, isPass};
	}

    /**
     * <pre>
     * 读取权限出来授权时，要取高一级授权级别的来授予低级别的。 
     * 比如原本【可授权的权限】可以拿出来进行【普通授权】， 
     * 原本【可授权授权且可传递授权】则可以拿出来进行【普通授权】或【可授权授权】， 
     * 而原本就是【普通授权】的权限则不能拿出来进行授权。 
     * </pre>
     * @param permissionRank
     * @return
     */
    String genRankCondition4SelectPermission(String permissionRank){
        if (UMConstants.LOWER_PERMISSION.equals(permissionRank)) {
            return " and p.isGrant=1 "; // 普通授权 : isGrant = 1
        }
        if(UMConstants.AUTHORISE_PERMISSION.equals(permissionRank) 
        		|| UMConstants.PASSON_AUTHORISE_PERMISSION.equals(permissionRank)) {
            return " and p.isGrant=1 and p.isPass=1";  // 可授权授权且可传递授权 : isGrant = 1 and isPass = 1
        }
        return "";
    }
    
	/**
	 * <p>
	 * 获取用户对一个资源在当前授权级别能够看到的子节点的个数
	 * </p>
	 * @param resourceId
	 * @param permissionRank
	 * @param permissionTable
	 * @param resourceTable
	 * @return
	 */
	public Map<Long, Integer> getVisibleChildrenNum(String permissionRank, String permissionTable, String resourceTable) {
		String hql = "select r.parentId, count(distinct r.id) from " + resourceTable + " r, " + permissionTable + " p, Temp t " +
				" where t.thread=" + Environment.threadID() + 
				"   and t.id = p.roleId and p.resourceId = r.id";
		hql += genRankCondition4SelectPermission(permissionRank);
		hql += " group by r.parentId ";
		
		List<?> list = getEntities(hql);
		Map<Long, Integer> map = new HashMap<Long, Integer>();
		for(Object obj : list) {
			Object[] objs = (Object[]) obj;
			map.put( EasyUtils.obj2Long(objs[0]), EasyUtils.obj2Int(objs[1]));
		}
		return map;
	}

	public Map<Long, Integer> getChildrenNum(String resourceTable) {
		List<?> list = getEntities("select parentId, count(distinct id) from " + resourceTable + " group by parentId");

		Map<Long, Integer> map = new HashMap<Long, Integer>();
		for(Object obj : list) {
			Object[] objs = (Object[]) obj;
			map.put( EasyUtils.obj2Long(objs[0]), EasyUtils.obj2Int(objs[1]));
		}
		return map;
	}
	
	/**
	 * <p>
	 * 获取用户资源树（根据登陆用户拥有的权限过滤，Temp临时表中保存了登陆用户的roleIds）
	 * </p>
	 * @param permissionRank
	 * @param permissionTable
	 * @param resourceTable
	 * @return
	 */
	public List<ResourceTreeNode> getVisibleResourceTree(String permissionRank, String permissionTable, String resourceTable) {
		String hql = "select distinct r.id, r.parentId, r.name, r.decode "
		    + " from " + resourceTable + " r, " + permissionTable + " p, Temp t " 
		    + " where t.thread=" + Environment.threadID() + " and t.id=p.roleId and p.resourceId = r.id ";
        hql += genRankCondition4SelectPermission(permissionRank) + " order by r.decode";

		return ResourceTreeNode.genResourceTreeNodeList(getEntities(hql));
	}

	/**
	 * <p>
	 * 获取补全的用户资源权限列表（根据登陆用户拥有的权限过滤，Temp临时表中保存了登陆用户的roleIds）
	 * </p>
     * @param permissionRank
     * @param permissionTable
     * @return
     */
    public List<PermissionDTO> getAllResourcePermissions(String permissionRank, String permissionTable) {
        String hql = "select distinct p.resourceId, p.operationId, p.permissionState, max(p.isGrant), max(p.isPass), p.roleId"
                + " from " + permissionTable + " p , Temp t " 
                + " where t.thread=" + Environment.threadID() + " and t.id=p.roleId " 
                + genRankCondition4SelectPermission(permissionRank) 
                + " group by p.resourceId, p.operationId, p.permissionState, p.roleId";
        
        return PermissionDTO.genPermissionDTOList(getEntities(hql));
    }

	/**
	 * <p>
	 * 获取用户对一个资源的权限信息
	 * </p>
	 * @param permissionRank
	 * @param permissionTable
	 * @param resourceId
	 * @return
	 */
	public List<PermissionDTO> getOneResourcePermissions(String permissionRank, String permissionTable, Long resourceId) {
		String hql = "select distinct p.resourceId, p.operationId, p.permissionState, max(p.isGrant), max(p.isPass), p.roleId"
				+ " from " + permissionTable + " p, Temp t " 
				+ " where t.thread=" + Environment.threadID() + " and t.id=p.roleId and p.resourceId = ?1 " 
				+ genRankCondition4SelectPermission(permissionRank) 
				+ " group by p.resourceId, p.operationId, p.permissionState, p.roleId";

		return PermissionDTO.genPermissionDTOList(getEntities(hql, resourceId));
	}

	// ===========================================================================
    // 用于资源权限过滤
    // ===========================================================================
	
    /**
     * 获取操作用户有指定操作权限（维护、浏览、查看等）的所有某类型资源ID列表
     * @param permissionTable 
     *                  资源权限表
     * @param operation    
     *                  操作权限
     * @param operatorId
     *                  操作人
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Long> getResourceIdsByOperation(String permissionTable, String operation, Long operatorId){
    	operatorId = (Long) EasyUtils.checkNull(operatorId, Anonymous.one.getId());
        String hql = "select distinct p.resourceId from RoleUserMapping ur, " + permissionTable + " p" +
              " where p.operationId = ?1 and p.roleId = ur.id.roleId and ur.id.userId = ?2 ";
        return (List<Long>) getEntities( hql, operation, operatorId );
    }
    
    public List<Long> getResourceIdsByOperation(String permissionTable, String operationId){
        return getResourceIdsByOperation(permissionTable, operationId, Environment.getUserId());
    }
    
    public List<Long> getResourceIdsByOperation(String appId, String resourceTypeId, String operationId){
		return getResourceIdsByOperation(appId, resourceTypeId, operationId, Environment.getUserId());
    }
    
    public List<Long> getResourceIdsByOperation(String appId, String resourceTypeId, String operationId, Long operatorId){
        String permissionTable = resourceTypeDao.getPermissionTable(appId, resourceTypeId);
		return getResourceIdsByOperation(permissionTable, operationId, operatorId);
    }
    
    /**
     * 把用户没有指定操作权限的资源给过滤点，资源列表里只留下有指定操作权限的。
     */
    public void filtrateResourcesByPermission( List<Long> permitedResourceIds, List<?> resources ){
        for( Iterator<?> it = resources.iterator(); it.hasNext(); ) {
            IEntity resource = (IEntity)it.next();
            if( !permitedResourceIds.contains(resource.getPK()) ) {
                it.remove();
            }
        }
    }
    
    public void filtrateResourcesByPermission(String appId, String resourceTypeId, String operationId, List<?> resources){
        String permissionTable = resourceTypeDao.getPermissionTable(appId, resourceTypeId);
        List<Long> permitedResourceIds = getResourceIdsByOperation(permissionTable, operationId);
        filtrateResourcesByPermission(permitedResourceIds, resources);
    }
    
    /**
     * 检查用户对某个资源是否有指定的操作项权限
     */
    public boolean checkPermission(Long resourceId, String permissionTable, Class<?> resourceClass, String...options) {
		List<String> permissions = getOperationsByResource(resourceId, permissionTable, resourceClass);
		for(String option : options) {
			if( permissions.contains( option ) ) return true;
		}
		return false;
	}
    
    /**
     * 获取资源的父节点Id
     * @param appId
     * @param resourceTypeId
     * @param resourceId
     * @return
     */
    public Long getParentResourceId(String appId, String resourceTypeId, Long resourceId) {
    	String resourceTable = resourceTypeDao.getResourceTable(appId, resourceTypeId);
    	IResource resource = (IResource) getEntity(BeanUtil.createClassByName(resourceTable), resourceId);
    	return resource.getParentId();
    }
    
    // 检查用户是否包含指定角色中的一个
	public static boolean checkRole(String roles) {
		
		List<Object> ownRoles = new ArrayList<Object>();
		ownRoles.addAll(Environment.getOwnRoles());
		ownRoles.addAll(Environment.getOwnRoleNames());

		return EasyUtils.contains(ownRoles, roles);
	}
	
	public static void vsSize(List<?> permitedList, List<?> list, String msg ) {
		if ( list.size() > permitedList.size() ) {
			throw new BusinessException(msg);
		}
	}
}

	