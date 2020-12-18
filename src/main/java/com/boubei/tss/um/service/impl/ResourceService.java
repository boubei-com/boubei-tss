/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IApplicationDao;
import com.boubei.tss.um.dao.IResourceTypeDao;
import com.boubei.tss.um.entity.Application;
import com.boubei.tss.um.entity.Operation;
import com.boubei.tss.um.entity.ResourceType;
import com.boubei.tss.um.entity.ResourceTypeRoot;
import com.boubei.tss.um.permission.PermissionService;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.XMLDocUtil;

@Service("ResourceService")
public class ResourceService implements IResourceService{
	
	@Autowired private IApplicationDao    applicationDao;
	@Autowired private IResourceTypeDao   resourceTypeDao;
	@Autowired private PermissionService  permissionService;

    public Object[] findApplicationAndResourceType() {
        // 应用系统列表
        List<?> apps = getApplications();
        
        // 资源类型列表
        List<?> resourceTypes = resourceTypeDao.getEntities("from ResourceType o order by o.seqNo");    
        
        // 操作项列表
        List<?> operations = resourceTypeDao.getEntities("from Operation o order by o.seqNo");    
        
        return new Object[]{apps, resourceTypes, operations};
    }

	public Application getApplication(String applicationId){
		return applicationDao.getApplication(applicationId);
	}
    
    public Application getApplicationById(Long id) {
        return applicationDao.getEntity(id);
    }

    public ResourceType getResourceTypeById(Long id) {
        return resourceTypeDao.getEntity(id);
    }

    public ResourceTypeRoot findResourceTypeRoot(String applicationId, String resourceTypeId){
        return resourceTypeDao.getResourceTypeRoot(applicationId, resourceTypeId);
    }
    
    public Operation getOperationById(Long id) {
        return (Operation) resourceTypeDao.getEntity(Operation.class, id);
    }

	public void removeApplication(Long id) {
		Application application = applicationDao.getEntity(id);
		applicationDao.clearDirtyData(application.getApplicationId());		
	}

	public void removeResourceType(Long id) {
		ResourceType resourceType = resourceTypeDao.getEntity(id);
		
        // 删除Operation表
		List<?> operationList = resourceTypeDao.getEntities("from Operation o where o.resourceTypeId = ?1", resourceType.getResourceTypeId());
		for(Object obj : operationList) {
		    removeOperation(((Operation)obj).getId());
		}
        
		resourceTypeDao.delete(resourceType);	
	}

	public void removeOperation(Long id) {
		Operation operation = getOperationById(id);
		resourceTypeDao.delete( operation );
	        
        // 删除权限选项, 连同删除RoleResourceOperation表中相关数据 
        ResourceType resourceType = resourceTypeDao.getResourceType(operation.getApplicationId(), operation.getResourceTypeId());
        String permissionTable = resourceType.getPermissionTable();
        String operationId = operation.getOperationId();
		resourceTypeDao.deleteAll(resourceTypeDao.getEntities("from " + permissionTable   + " o where o.operationId = ?1", operationId));
	}
    
	public void saveApplication(Application application) {
		if( application.getId() == null ){ // 新建
			applicationDao.create(application);
		} 
		else {
			applicationDao.update(application);
		}
	}

    public Object createResourceType(ResourceType resourceType){
        resourceTypeDao.create(resourceType);
		
		// 保存一个应用系统中一种类型的根节点
		ResourceTypeRoot resourceTypeRoot = new ResourceTypeRoot();
		resourceTypeRoot.setApplicationId(resourceType.getApplicationId());
		resourceTypeRoot.setResourceTypeId(resourceType.getResourceTypeId());
		resourceTypeRoot.setRootId(resourceType.getRootId());
		resourceTypeDao.createObject(resourceTypeRoot);
		
		return resourceType;
	}
    
    public Object updateResourceType(ResourceType resourceType) {
        String applicationId = resourceType.getApplicationId();
        String resourceTypeId = resourceType.getResourceTypeId();
        ResourceTypeRoot resourceTypeRoot = resourceTypeDao.getResourceTypeRoot(applicationId, resourceTypeId);
        resourceTypeRoot.setRootId(resourceType.getRootId());
        resourceTypeDao.update(resourceType);
        
        return resourceType;
    }
    
    public Operation saveOperation(Operation operation){
    	resourceTypeDao.createObject(operation);
        String applicationId = operation.getApplicationId();
        String resourceTypeId = operation.getResourceTypeId();
        
        ResourceTypeRoot resourceTypeRoot = resourceTypeDao.getResourceTypeRoot(applicationId, resourceTypeId);
        String permissionTable = resourceTypeDao.getPermissionTable(applicationId, resourceTypeId);
        String resourceTable = resourceTypeDao.getResourceTable(applicationId, resourceTypeId);
        
        // 新建的权限选项要将该权限选项赋予管理员角色(id==-1)
        permissionService.saveRoleResourceOperation(UMConstants.ADMIN_ROLE_ID, resourceTypeRoot.getRootId(), 
                operation.getOperationId(), UMConstants.PERMIT_SUB_TREE, permissionTable, resourceTable);
            
        return operation;
    }
    
    public void updateOperation(Operation operation) {
    	resourceTypeDao.update(operation);
    }

    public List<?> getApplications() {
    	return applicationDao.getEntities("from Application o order by o.id");
    }
 
    public void applicationResourceRegister(Document doc, String applicationType) {
       
        List<ResourceType> resourceTypeList = new ArrayList<ResourceType>();
        List<ResourceTypeRoot> resourceTypeRootList = new ArrayList<ResourceTypeRoot>();
        List<Operation> operationList = new ArrayList<Operation>();
        
        // 解析应用
        Application application = new Application();
        Element appNode = (Element) doc.selectSingleNode("/application");
        BeanUtil.setDataToBean(application, XMLDocUtil.dataNode2Map(appNode));
        
        String applicationId = application.getApplicationId();
        
        // 解析资源类型
        List<Element> nodeList = XMLDocUtil.selectNodes(appNode, "resourceType");
        for (Element resourceTypeNode : nodeList) {
            ResourceType resourceType = new ResourceType();
            BeanUtil.setDataToBean(resourceType, XMLDocUtil.dataNode2Map(resourceTypeNode));
            resourceType.setApplicationId(applicationId);
            resourceTypeList.add(resourceType);
            
            // 解析资源操作选项
            List<Element> operationNodeList = XMLDocUtil.selectNodes(resourceTypeNode, "operation");
            for (Element operationNode : operationNodeList) {
                Operation operation = new Operation();
                BeanUtil.setDataToBean(operation, XMLDocUtil.dataNode2Map(operationNode));
                operation.setApplicationId(applicationId);
                operation.setResourceTypeId(resourceType.getResourceTypeId());
                operationList.add(operation);
            }
        }
        
        // 解析资源类型根节点
        nodeList = XMLDocUtil.selectNodes(appNode, "resourceTypeRoot");
        for (Element resourceTypeRootNode : nodeList) {
            ResourceTypeRoot resourceTypeRootId = new ResourceTypeRoot();
            BeanUtil.setDataToBean(resourceTypeRootId, XMLDocUtil.dataNode2Map(resourceTypeRootNode));
            resourceTypeRootList.add(resourceTypeRootId);
        }           
        
        application.setApplicationType(applicationType);         
        
        /*****************************  仅仅把外部资源注册进来,不进行权限补全操作 *******************************/
        
        // 根据应用删除上次因导入失败产生的脏数据
        applicationDao.clearDirtyData(applicationId);
        
        for(IEntity resourceTypeRoot : resourceTypeRootList) {
        	resourceTypeDao.createObject(resourceTypeRoot); // 初始化资源类型根节点
        }
                  
        for (ResourceType resourceType : resourceTypeList) {
            String resourceTypeId = resourceType.getResourceTypeId();
            ResourceTypeRoot resourceTypeRoot = resourceTypeDao.getResourceTypeRoot(applicationId, resourceTypeId);
            resourceType.setRootId(resourceTypeRoot.getRootId());
            resourceTypeDao.create(resourceType); // 初始化资源类型
        }
        
        for (Operation operation : operationList) {     
        	resourceTypeDao.createObject(operation); // 初始化权限选项
        }       
        
        /*****************************  对外部已经注册的资源进行补全操作 ************************************/

        // 初始化权限选项
        for (Operation operation : operationList) {         
            /* 让管理员角色拥有新添加的权限选项，即:让管理员拥有对资源 根节点 有permissionState=2的权限 */
            String resourceTypeId = operation.getResourceTypeId();
            ResourceTypeRoot resourceTypeRoot = resourceTypeDao.getResourceTypeRoot(applicationId, resourceTypeId);
            String permissionTable = resourceTypeDao.getPermissionTable(applicationId, resourceTypeId);
            String resourceTable   = resourceTypeDao.getResourceTable(applicationId, resourceTypeId);
            permissionService.saveRoleResourceOperation(UMConstants.ADMIN_ROLE_ID, resourceTypeRoot.getRootId(), 
                    operation.getOperationId(), UMConstants.PERMIT_SUB_TREE, 
                    permissionTable, resourceTable);
        }
        
        // 最后保存平台应用系统
        applicationDao.create(application); 
    }
}
	