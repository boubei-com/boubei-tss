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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Operation;
import com.boubei.tss.um.permission.dispaly.ResourceTreeNode;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
 
/**
 * <pre>
 * ===========================================================================
 * 展示外部资源的授权信息时需要的操作：
 * 1. 从UM中取到当前用户的角色信息
 * 2. 保存到登录系统的临时表当中
 * 3. 展示资源时关联该临时表
 * ===========================================================================
 * </pre>
 */
@Service("PermissionService")
public class PermissionServiceImpl implements PermissionService {
	
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired private PermissionHelper pHelper;
	@Autowired private RemoteResourceTypeDao remoteRTDao;

	public void clearPermissionData(String permissionTable) {
		pHelper.executeHQL("delete " + permissionTable);
	}
	
	public void clearPermissionByRole(String appId, String resourceTypeId, 
			String permissionRank, Long roleId, Integer isRole2Resource) {
		
        String permissionTable = remoteRTDao.getPermissionTable(appId, resourceTypeId);
		
		String rankCondition = pHelper.genRankCondition4DeleletePermission(permissionRank);
		String field = ParamConstants.TRUE.equals(isRole2Resource) ? "roleId" : "resourceId";
 
        pHelper.executeHQL("delete " + permissionTable + " p where" + " p." + field + "=?1 " + rankCondition, roleId);
        
        pHelper.flush();
	}
	
	public void saveResources2Role(String appId, String resourceTypeId, Long roleId, String permissionRank, String permissions) {
        String permissionTable = remoteRTDao.getPermissionTable(appId, resourceTypeId);// 资源权限的表名
        String resourceTable = remoteRTDao.getResourceTable(appId, resourceTypeId);   // 资源表的表名或视图名
        List<?> operationIds = remoteRTDao.getOperationIds(appId, resourceTypeId);
        
		pHelper.deletePermissionByRole(roleId, permissionRank, permissionTable, resourceTable);
		log.debug("delete exsits permissions by role.");
       
		Integer[] isGrantAndPass = pHelper.convertRank(permissionRank);
        String[] resources2opts = permissions.split(",");
		for ( String resource2Opts : resources2opts ) {  // 格式如 resource1|2224
            if ( EasyUtils.isNullOrEmpty(resource2Opts) ) continue; 
            
			int index = resource2Opts.indexOf("|");
			Long resourceId = Long.valueOf(resource2Opts.substring(0, index).trim());
			String optStates = resource2Opts.substring(index + 1);
            for (int i = 0; i < operationIds.size(); i++) {
                /* 0:没打勾, 1:仅此节点, 2:此节点并包含所有子节点, 3:禁用未选中 4:禁用已选中 */
            	Integer pState = Character.getNumericValue(optStates.charAt(i)); 
            	
            	/* 只有打上了半勾 或 全勾，才生成授权信息。其他状态均无授权信息生成 */
                if (UMConstants.PERMIT_NODE_SELF.equals(pState) || UMConstants.PERMIT_SUB_TREE.equals(pState)) {
                    /* 创建一条未补全授权对象，用以后续补全权限 */
                	AbstractPermission unPermission = pHelper.createUnPermission(
                			roleId, resourceId, (String) operationIds.get(i), pState, 
			                isGrantAndPass[0], isGrantAndPass[1], permissionTable);
					
					supplyPermission(unPermission, permissionTable, resourceTable);    
				}
            }
		}
	}
	
    /**
     * <pre>
     * 保存角色资源权限表后补全的操作。 
     * 这里的补全操作是这样的:  
     * 如果对资源的授权状态permissionState==PERMIT_SUB_TREE，那么要找到该资源所有子节点，并且授予相应的权限选项； 
     * 如果permissionState==PERMIT_NODE_SELF，则只对自身节点在补齐表里加一条记录。 
     * </pre>
     * @param unPermission
     * @param permissionTable
     * @param resourceTable
     */
    private void supplyPermission(AbstractPermission unPermission, String permissionTable, String resourceTable) {
        Long resourceId = unPermission.getResourceId();
        
        // 如果是授予整枝节点，则补全表的所有子节点
		if (unPermission.getPermissionState().equals(UMConstants.PERMIT_SUB_TREE)) { 
            List<?> resources = pHelper.getChildrenById(resourceTable, resourceId);
            for (Object child : resources ) {
            	pHelper.createPermission(unPermission, (IResource) child, permissionTable);
            }
        } else {// 补全表的单个节点
            IResource resource = (IResource) pHelper.getEntity(BeanUtil.createClassByName(resourceTable), resourceId);
            pHelper.createPermission(unPermission, resource, permissionTable);
        }
    }
	
	public void saveResource2Roles(String appId, String resourceTypeId, Long resourceId, String permissionRank, String permissions) {
        String permissionTable = remoteRTDao.getPermissionTable(appId, resourceTypeId); // 资源权限的表名
        String resourceTable = remoteRTDao.getResourceTable(appId, resourceTypeId);    // 资源表的表名或视图名
        List<?> operations   = remoteRTDao.getOperations(appId, resourceTypeId);
        
        pHelper.deletePermissionByResource(resourceId, permissionRank, permissionTable, resourceTable);
 
		Integer[] isGrantAndPass = pHelper.convertRank(permissionRank);
        String[] resources2opts = permissions.split(",");
        for ( String resource2Opts : resources2opts ) {  // 格式如 resource1|2224
            if ( EasyUtils.isNullOrEmpty(resource2Opts) ) continue; 
            
            resource2Opts = resource2Opts.trim();
			int index = resource2Opts.indexOf("|");
			Long roleId = Long.valueOf(resource2Opts.substring(0, index));
            String optStates = resource2Opts.substring(index + 1);
            
            /* 授权后台检测，创建完整的授权信息。
             * 在创建单个资源对某操作选项的授权信息的时同时检测该操作选项是否是否纵向或者横向依赖，如果是，则同时创建依赖的首选信息。
             */
            for (int i = 0; i < operations.size(); i++) {
            	Operation operation = (Operation) operations.get(i);
            	String operationId = operation.getOperationId();
            	Integer permissionState = Character.getNumericValue(optStates.charAt(i)); // 0:没打勾， 1:仅此节点，2:此节点并包含所有子节点， 3:禁用未选中， 4:禁用已选中
            	// 只有打上了半勾 或 全勾，才生成授权信息。其他状态均无授权信息生成
                if (UMConstants.PERMIT_NODE_SELF.equals(permissionState) || UMConstants.PERMIT_SUB_TREE.equals(permissionState)) { 
                	// 新增当前资源节点的授权信息
                	createAndSupplyPermission(permissionTable, resourceTable, roleId, resourceId, operationId, permissionState, isGrantAndPass);
                	
                	// 权限项横向依赖的id，格式如： opt12
                	String dependId = operation.getDependId(); 
                    dependId = EasyUtils.isNullOrEmpty(dependId) ? null : dependId.substring(3);
                    
                    // 权限项纵向依赖的类型（1:向上兼向下，2:向上，3:向下）
                    String dependParent = operation.getDependParent(); 
                    
                    /* 
                     * 向上依赖的情形： 比如授予了某资源的“启用”权限给某角色，则同时需要把该资源的所有父节点的“启用”权限也一并授予该角色，否则启用时可能启用不了父节点。
                     * 对父亲节点的授权级别仅限于自己， permissionState = UMConstants.PERMIT_NODE_SELF。
                     */
                    if (UMConstants.DEPEND_UP.equals(dependParent) || UMConstants.DEPEND_UP_DOWN.equals(dependParent)) {
                    	List<?> parentResources = pHelper.getParentsById(resourceTable, resourceId);
                        for(Object temp : parentResources){
                            IResource parent = (IResource) temp;
                            Long parentId = parent.getId();
							createAndSupplyPermission(permissionTable, resourceTable, roleId, parentId, operationId, 
									UMConstants.PERMIT_NODE_SELF, isGrantAndPass);
                            
							if(dependId != null) { // 补上针对【横向依赖权限选项】的授权信息，比如授了“启用”权限，需要把“启用”依赖的“查看”权限也一起授予（要启用首先要有查看权限）。
                                createAndSupplyPermission(permissionTable, resourceTable, roleId, parentId, dependId, 
                                		UMConstants.PERMIT_NODE_SELF, isGrantAndPass);
                            }
                        }
                    }
                    
                    // 如果是全构，则无需补全子节点，自动会补齐
                    if(UMConstants.PERMIT_SUB_TREE.equals(permissionState)) continue;
                    
                    /* 
                     * 向下依赖的情形： 比如授予了某资源的“停用”权限给某角色，则同时需要把该资源的所有子节点的“停用”权限也一并授予该角色，否则停用时可能停用不了子节点。
                     * 经上一行代码判断过滤，执行到这里的 permissionState 必等于 UMConstants.PERMIT_NODE_SELF。
                     */
                    if (UMConstants.DEPEND_DOWN.equals(dependParent) || UMConstants.DEPEND_UP_DOWN.equals(dependParent)) {
                    	List<?> childResources = pHelper.getChildrenById(resourceTable, resourceId);
                    	for(Object temp :childResources ){
                            IResource child = (IResource) temp;
                            Long childId = child.getId();
							createAndSupplyPermission(permissionTable, resourceTable, roleId, childId, operationId, UMConstants.PERMIT_NODE_SELF, isGrantAndPass);
                            if(dependId != null) {  // 补上针对【横向依赖权限选项】的授权信息，比如授了“停用”权限，需要把“停用”依赖的“查看”权限也一起授予（要停用首先要能看到呗）。
                                createAndSupplyPermission(permissionTable, resourceTable, roleId, childId, dependId, UMConstants.PERMIT_NODE_SELF, isGrantAndPass);
                            }
                        }
                    }
                }
            }
		}
	}
 
    private void createAndSupplyPermission(String permissionTable, String resourceTable, 
    		Long roleId, Long resourceId, String operationId, Integer permissionState, Integer[] isGrantAndPass){
    	
    	AbstractPermission unPermission = pHelper.createUnPermission(
    			roleId, resourceId, operationId, permissionState, 
                isGrantAndPass[0], isGrantAndPass[1], permissionTable);
       
        supplyPermission(unPermission, permissionTable, resourceTable);    
    }

    // 创建角色、资源、权限选项关联关系。 添加权限选项，给管理员添加最大权限时使用
	public void saveRoleResourceOperation(Long roleId, Long resourceId, String operationId, Integer permissionState, 
			String permissionTable, String resourceTable) {
	    
		AbstractPermission unPermission = pHelper.createUnPermission(
                roleId, resourceId, operationId, permissionState, 
                ParamConstants.TRUE, ParamConstants.TRUE, permissionTable); // 默认为可授权 且 可传递
        
        supplyPermission(unPermission, permissionTable, resourceTable); // 补全
	}
    
    // *******************************************************************************************************
    // *******************************    以下为获取 资源-操作选项矩阵 的方法    ***********************************
    // ******************************************************************************************************
    
	public Object[] genResource2OperationMatrix(String appId, String resourceTypeId, Long roleId, 
			String permissionRank, List<Long[]> roleUsers) {
		
		log.debug("往临时表中插入用户角色数据，用以获取资源时够过滤权限。");
		pHelper.insertIds2TempTable(roleUsers, 1);

		/* ----------------------------------------- 获取角色对资源授权矩阵数据 -----------------------------------------------*/
        String permissionTable = remoteRTDao.getPermissionTable(appId, resourceTypeId);// 资源权限的表名
        String resourceTable = remoteRTDao.getResourceTable(appId, resourceTypeId);   // 资源表的表名或视图名
		List<?> operations   = remoteRTDao.getOperations(appId, resourceTypeId);     // 此类资源的所有操作选项
        
		log.debug("获取用户资源树，根据登陆用户拥有的权限过滤");
        List<ResourceTreeNode> visibleResourceTree = pHelper.getVisibleResourceTree(permissionRank, permissionTable, resourceTable);
        
        /* 当前用户拥有的补全的用户资源权限信息，将登录用户拥有的资源权限列表和授权级别进行映射。
         * 即把用户拥有的权限取出以部分或全部授予选中的角色，界面上体现为是否能在对应位置打勾
         */
        List<PermissionDTO> exsitPermissions = pHelper.getAllResourcePermissions(permissionRank, permissionTable);
        Map<String, Integer> userPermissionMappingRank = userPermissionMappingRank(exsitPermissions); // 这是登录用户能拿出来授权的【针对当前资源列表】的权限
        
        /* 将选中角色（角色树上选中来【角色权限设置】的那个节点 ）拥有的资源授权信息和授权级别进行映射。
         * 目的是把角色对资源已有的权限信息展示到界面上，界面上体现为对应的位置已经打上勾。
         */
        List<?> roleResList = pHelper.getEntities("from " + permissionTable + " p where p.roleId = ?1 ",  roleId);
        Map<String, Integer[]> rolePermissionMappingRank = rolePermissionMappingRank(roleResList); // 这是【被授权角色】已经拥有的对【当前资源列表】的权限。
   
        /* 将用户可见的资源和其【permissionState：权限维护状态(1-仅此节点，2-该节点及所有下层节点)】进行映射。
         * 授权打勾时 pState 1：只能打“仅此节点”的半勾 2：可打“全勾”，来选中所有子节点
         */
        Map<Long, Integer> resourceMappingRank = new HashMap<Long, Integer>();
        Map<Long, Integer> childNumMap  = pHelper.getChildrenNum(resourceTable);
        Map<Long, Integer> childNumMap2 = pHelper.getVisibleChildrenNum(permissionRank, permissionTable, resourceTable);
        for (ResourceTreeNode resource : visibleResourceTree) {
            Long resourceId = resource.getResourceId();
            int totalCount 	= EasyUtils.obj2Int( childNumMap.get(resourceId) );
            int permitedCount = EasyUtils.obj2Int( childNumMap2.get(resourceId) );
            
            // 用户是否对此资源的所有的子节点拥有权限，是的话可以打“全勾”
            Integer permissionState = calState(totalCount, permitedCount);
            resourceMappingRank.put(resourceId, permissionState);
        }
        
        appendOptionInfo2LeftTree(true, roleId, permissionRank, operations, visibleResourceTree, 
                userPermissionMappingRank, rolePermissionMappingRank, resourceMappingRank);
       
        return new Object[] { visibleResourceTree, operations };
	}

    /**
     * 循环资源树(visibleResourceTree)和操作权限列(operations)，在权限项上打钩或者disable，生成最终的二维树 
     * 
     * @param isRoleId
     *              选中授权的节点是否为角色，true：角色【角色权限设置】，false：资源【资源授予角色】
     * @param selectNodeId
     *              选中授权的节点的ID
     * @param permissionRank
     *              授权级别
     * @param operations
     *              此类资源的所有操作选项
     * @param leftTree
     *              授权矩阵左边的树，资源树 or 角色树
     * @param userPermissionMappingRank
     *              登录用户能拿出来授权的【针对当前资源或资源列表】的权限
     * @param rolePermissionMappingRank
     *              【被授权角色或角色列表】已经拥有的对【当前资源或资源列表】的权限
     * @param resourceMappingRank
     *              资源和其【权限维护状态(1-仅此节点，2-该节点及所有下层节点)】进行映射
     *              以用于判断用户是否对【此资源的所有的子节点】拥有权限，是的话可以打“全勾”；否则只能打“半勾”。
     */
    private void appendOptionInfo2LeftTree(boolean isRoleId, Long selectNodeId, String permissionRank,  
            List<?> operations, List<ResourceTreeNode> leftTree, Map<String, Integer> userPermissionMappingRank, 
            Map<String, Integer[]> rolePermissionMappingRank, Map<Long, Integer> resourceMappingRank) {
        
        Integer[] isGrantAndPass = pHelper.convertRank(permissionRank);
        Integer grantAndPass = MathUtil.addInteger(isGrantAndPass[0], isGrantAndPass[1]);  // 0 or 1 or 2，当前授权级别
        for (ResourceTreeNode tempNode : leftTree) {
            Long roleId;
            Long resourceId;
            if(isRoleId) {
                roleId = selectNodeId;
                resourceId = tempNode.getResourceId();
            } 
            else {
                roleId = tempNode.getResourceId();
                resourceId = selectNodeId;
            }
            
            tempNode.putOptionAttribute("pstate", resourceMappingRank.get(resourceId)); // 该值决定能否打全勾
            
            for ( Object temp : operations ) { /* -----------  打勾 ---------- */
                String operationId = ((Operation) temp).getOperationId();
                String key = resourceId + "_" + operationId;
                
                // 如果userPermissionMappingRank里面查找不到，说明登录用户对【该资源】的【该选项】没有授权权限
                String optionAttributeKey = "opt" + operationId;
                if (!userPermissionMappingRank.containsKey(key)) {
                    tempNode.putOptionAttribute(optionAttributeKey, 3);  // opt**=3，disabled，不让打勾
                    continue;
                } 
 
                // 判断【该角色】是否对该【该资源】的【该选项】已有权限，有的话视情况打勾，没有则不打勾。
                Integer[] rankInfo = rolePermissionMappingRank.get(roleId + "_" + key);
                if(rankInfo == null) { // 没授过权
                    tempNode.putOptionAttribute(optionAttributeKey, 0); // opt**=0, 没打勾 
                } 
                else if (rankInfo[0] > grantAndPass) { // 更好级别的授权不能再低级别授权里去掉
                    tempNode.putOptionAttribute(optionAttributeKey, 4); // 大于：opt**=4, 打勾并disabled，不让去掉授权
                } 
                else if (rankInfo[0] < grantAndPass) { // 已有权限的授权级别低于当前的授权级别，不做为当前级别的授权
                    tempNode.putOptionAttribute(optionAttributeKey, 0); // 小于：opt**=0, 没打勾
                }
                else if (rankInfo[0].equals(grantAndPass)) { // 当前的授权级别下，角色对资源已经拥有权限
                    tempNode.putOptionAttribute(optionAttributeKey, rankInfo[1]); // 相等：opt**=1--半勾 或 opt**=2--全勾
                }
            }
        }
    }
    
    /**
     * <pre>
     * 将登录用户拥有的资源权限列表和授权级别进行映射。 
     * 这是登录用户能拿出来授权的【针对当前资源或资源列表】的权限。
     * key:resourceId_operationId / value:isGrant+isPass(角色为选中的唯一一个)
     * </pre> 
     */
    private Map<String, Integer> userPermissionMappingRank(List<PermissionDTO> permissionList) {
        Map<String, Integer> mapping = new HashMap<String, Integer>();
        for (PermissionDTO dto : permissionList) {
            Integer grantAndPass = MathUtil.addInteger(dto.getIsGrant(), dto.getIsPass());  // 0 or 1 or 2
            
            String key = dto.getResourceId() + "_" + dto.getOperationId();
            Integer maxRank =  mapping.get(key);
            // 保存最大的授权级别（按 【可授权可传递（2）-->可授权（1）-->普通授权（0）】 的次序递减）
            if(maxRank == null || grantAndPass > maxRank) {
                mapping.put(key, grantAndPass); 
            }
        }
        return mapping;
    }
    
    /**
     * <pre>
     * 将选中角色拥有的资源权限列表和授权级别进行映射。
     * 这是【被授权角色或角色列表】已经拥有的对【当前资源或资源列表】的权限。
     * key:roleId_resourceId_operationId / value:isGrant+isPass 
     * </pre> 
     */
    private Map<String, Integer[]> rolePermissionMappingRank(List<?> permissionList) {
        Map<String, Integer[]> mapping = new HashMap<String, Integer[]>();
        
        permissionList = (List<?>) EasyUtils.checkNull(permissionList, new ArrayList<Object>());
        for ( Object temp : permissionList ) {
        	AbstractPermission permission = (AbstractPermission) temp;
            Integer grantAndPass = MathUtil.addInteger(permission.getIsGrant(), permission.getIsPass());  // 0 or 1 or 2
            
            String key = permission.getRoleId()  + "_" + permission.getResourceId()  + "_" + permission.getOperationId();
            Integer[] maxRank =  mapping.get(key);
            // 保存最大的授权级别（按 【可授权可传递（2）-->可授权（1）-->普通授权（0）】 的次序递减）
            if(maxRank == null || grantAndPass > maxRank[0]) {
                mapping.put(key, new Integer[]{grantAndPass, permission.getPermissionState()});
            }
        }
        return mapping;
    }

    //*******************************************************************************************************
    //*******************************    以下为获取 角色-操作选项矩阵 的方法    ***********************************
    //******************************************************************************************************
	public Object[] genRole2OperationMatrix(String appId, String resourceTypeId, Long resourceId, 
			String permissionRank, List<Long[]> roleUsers) {
        
		log.debug("往临时表中插入用户角色数据，用以获取资源时够过滤权限。");
		pHelper.insertIds2TempTable(roleUsers, 1);
        
		/* ----------------------------------------- 获取资源对角色授权矩阵数据 -----------------------------------------------*/
		String permissionTable = remoteRTDao.getPermissionTable(appId, resourceTypeId); // "补全的表名"
        String resourceTable = remoteRTDao.getResourceTable(appId, resourceTypeId); // "资源表的表名或视图名"
		List<?> operations   = remoteRTDao.getOperations(appId, resourceTypeId);    // 此类资源的所有操作选项
		
		// 从UM里读取Role本身作为一种资源的相关信息
        String tssAppId = UMConstants.TSS_APPLICATION_ID;
        String roleResourceTable = remoteRTDao.getResourceTable(tssAppId, UMConstants.ROLE_RESOURCE_TYPE_ID); // 角色资源表名
        String rolePermissionTable = remoteRTDao.getPermissionTable(tssAppId, UMConstants.ROLE_RESOURCE_TYPE_ID); // 角色资源补全表名
        
        /* 获取登录用户对UM中的【角色资源】中有“角色权限设置”权限的那些个角色的ID集合（用于生成【资源授予角色】时的角色树） */
        String hql = "select distinct p.resourceId from " + rolePermissionTable + " p, RoleUserMapping ru " +
                " where p.roleId = ru.id.roleId and p.operationId = ?1 and ru.id.userId = ?2";
        List<?> setableRoleIds = pHelper.getEntities(hql, UMConstants.ROLE_EDIT_OPERRATION, Environment.getUserId());
        
        // 登录用户有“角色权限设置”权限的角色列表
		List<ResourceTreeNode> setableRoleTree = new ArrayList<ResourceTreeNode>();
		// 登录用户有“角色权限设置”权限的角色列表 对 选中资源的权限情况
        List<?> setableRolesPermissionsOnSelectedResource = null;
        if ( !EasyUtils.isNullOrEmpty(setableRoleIds) ){ // 获取用户的角色树
            hql = "select distinct o.id, o.parentId, o.name, o.decode from "
                + roleResourceTable + " o, " + rolePermissionTable + " p, Role r where p.resourceId = o.id and o.id in (:ids)"
            	+ " and o.id = r.id and r.isGroup = 0 "
                + pHelper.genRankCondition4SelectPermission(permissionRank) + " order by o.decode";

            List<?> list = pHelper.getEntities(hql, new String[]{"ids"}, new Object[]{setableRoleIds});
            setableRoleTree = ResourceTreeNode.genResourceTreeNodeList(list);  // 角色作为资源的树
            
            hql = "from " + permissionTable + " p where p.roleId in (:roleIds) and p.resourceId = :resourceId ";
            setableRolesPermissionsOnSelectedResource = pHelper.getEntities(hql,  
                    new String[]{"roleIds", "resourceId"}, new Object[]{setableRoleIds, resourceId});
        }
		
        /* 取出当前用户拥有的对【当前资源】的权限信息，将登录用户拥有的资源（仅指resourceId）权限列表和授权级别进行映射。
         * 即把用户拥有的【对此资源的权限】取出以部分或全部授予【角色树上的角色】，界面上体现为是否能在对应位置打勾
         */
		List<PermissionDTO> exsitPermissions = pHelper.getOneResourcePermissions(permissionRank, permissionTable, resourceId);
		Map<String, Integer> userPermissionMappingRank = userPermissionMappingRank(exsitPermissions); // 这是登录用户能拿出来授权的【针对当前资源】的权限
 
        /* 将选中资源（资源树上选中来【授予角色】的那个节点 ）被拥有的资源授权信息和授权级别进行映射。
         * 目的是把角色对资源已有的权限信息展示到界面上，界面上体现为对应的位置已经打上勾。
         */
		// 这是【被授权角色列表】已经拥有的对【当前资源】的权限。
		Map<String, Integer[]> rolePermissionMappingRank = rolePermissionMappingRank(setableRolesPermissionsOnSelectedResource); 
	
		/* 将选中资源和其【permissionState：权限维护状态(1-仅此节点，2-该节点及所有下层节点)】进行映射。
		 * 以用于判断用户是否对【此资源的所有的子节点】拥有权限，是的话可以打“全勾”；否则只能打“半勾”。
		 */
		Map<Long, Integer> resourceMappingRank = new HashMap<Long, Integer>();
		
		Map<Long, Integer> childNumMap  = pHelper.getChildrenNum(resourceTable);
        Map<Long, Integer> childNumMap2 = pHelper.getVisibleChildrenNum(permissionRank, permissionTable, resourceTable);
        int totalCount 	= EasyUtils.obj2Int( childNumMap.get(resourceId) );
        int permitedCount = EasyUtils.obj2Int( childNumMap2.get(resourceId) );
		
        Integer permissionState = calState(totalCount, permitedCount);
        resourceMappingRank.put(resourceId, permissionState);
        
        appendOptionInfo2LeftTree(false, resourceId, permissionRank, operations, setableRoleTree, 
                userPermissionMappingRank, rolePermissionMappingRank, resourceMappingRank);
		
		return new Object[] { setableRoleTree, operations };
	}

	Integer calState(int totalCount, int permitedCount) {
		return permitedCount == totalCount ? UMConstants.PERMIT_SUB_TREE : UMConstants.PERMIT_NODE_SELF;
	}
}