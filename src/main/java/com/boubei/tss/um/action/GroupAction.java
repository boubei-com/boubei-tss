/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.boubei.tss.framework.web.display.tree.ITreeTranslator;
import com.boubei.tss.framework.web.display.tree.LevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.framework.web.mvc.ProgressActionSupport;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.GroupTreeParser;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.EasyUtils;
 
@Controller
@RequestMapping("/auth/group")
public class GroupAction extends ProgressActionSupport {

	@Autowired private IGroupService service;

	@RequestMapping("/list")
	public void getAllGroup2Tree(HttpServletResponse response) {
		List<?> groups = service.findGroups();
		TreeEncoder treeEncoder = new TreeEncoder(groups, new GroupTreeParser());
        treeEncoder.setNeedRootNode(false);
		
        print("GroupTree", treeEncoder);
	}
	
	@RequestMapping("/visibleList")
	public void getVisibleGroup2Tree(HttpServletResponse response) {
		List<?> groups = service.getVisibleSubGroups(UMConstants.MAIN_GROUP_ID);
		TreeEncoder encoder = new TreeEncoder(groups, new LevelTreeParser());
		encoder.setNeedRootNode(false);
		print("GroupTree", encoder);
	}

	@RequestMapping("/list/{type}")
    public void getCanAddedGroup2Tree(HttpServletResponse response, @PathVariable("type") int type) {
        String operationId = UMConstants.GROUP_EDIT_OPERRATION;
        
        TreeEncoder treeEncoder;
        
        if (Group.ASSISTANT_GROUP_TYPE.equals(type)) { // 辅助用户组
        	Object[] objs = service.getAssistGroupsByOperationId(operationId);
            treeEncoder = new TreeEncoder(objs[1], new LevelTreeParser());
        } 
        else { // 默认为主用户组
        	// 用户可能只对某些子组有权限，需要把这些子组的父节点也找出来，以组成一棵完成的组织结构树
        	Object[] objs = service.getMainGroupsByOperationId(operationId); 
            treeEncoder = new TreeEncoder(objs[1], new LevelTreeParser());

            final List<?> groupIds = (List<?>) objs[0];
            treeEncoder.setTranslator(new ITreeTranslator() {
                public Map<String, Object> translate(Map<String, Object> attribute) {
                    boolean f = groupIds.contains(attribute.get("id"));
                    attribute.put("canselected", EasyUtils.checkTrue(f, "1", "0"));
                    return attribute;
                }
            });
        }
        
    	treeEncoder.setNeedRootNode(false);
        print("GroupTree", treeEncoder);
    }
	
	/**
	 * 得到操作权限
	 */
	@RequestMapping("/operations/{resourceId}")
	public void getOperation(HttpServletResponse response, 
			@PathVariable("resourceId") Long resourceId) {
		
        String resourceTypeId = UMConstants.GROUP_RESOURCE_TYPE_ID;
        List<?> operations = PermissionHelper.getInstance().getOperationsByResource(resourceTypeId, resourceId);
 
        print("Operation", EasyUtils.list2Str(operations));
    }
	
	/**
	 * 根据用户组id查找用户列表
	 */
	@RequestMapping(value = "/users/{groupId}")
	public void getUserByGroupId(HttpServletResponse response, @PathVariable("groupId") Long groupId) {
		List<?> list = service.getUsersByGroupId(groupId);
		print("Group2UserListTree", new TreeEncoder(list));
	}
	
	/**
	 * 获取一个Group对象的明细信息、用户组对用户信息、用户组对角色的信息
	 */
	@RequestMapping(value = "/detail/{parentId}/{id}/{type}")
	public void getGroupInfo(HttpServletResponse response, 
			@PathVariable("parentId") Long parentId, 
			@PathVariable("id") Long id, 
			@PathVariable("type") int type) {
		
        Map<String, Object> groupAttributes;
        List<?> users;
        
		boolean isNew = UMConstants.DEFAULT_NEW_ID.equals(id);
        if(isNew) {
        	groupAttributes = new HashMap<String, Object>();
            groupAttributes.put("parentId", parentId);
            groupAttributes.put("groupType", type);
            users = new ArrayList<User>();
        } 
        else {
            Group group = service.getGroupById(id);
            groupAttributes = group.getAttributes4XForm();
            users = service.getUsersByGroupId(id);
        }
        
        TreeEncoder usersTreeEncoder = new TreeEncoder(users);
        
        String groupXForm = null;
        if(Group.MAIN_GROUP_TYPE.equals(type)) {
            groupXForm = UMConstants.GROUP_MAIN_XFORM;      // 主用户组
        }
        if(Group.ASSISTANT_GROUP_TYPE.equals(type)) {
            groupXForm = UMConstants.GROUP_ASSISTANT_XFORM; // 辅助用户组
        }
 
        XFormEncoder groupEncoder = new XFormEncoder(groupXForm, groupAttributes);
 
    	// 如果是新建则找到父组对应的角色，如此新建的子组可以继承父组角色
    	List<?> roles = service.findRolesByGroupId(isNew ? parentId : id); 
        TreeEncoder rolesTreeEncoder = new TreeEncoder(roles);
        
        TreeEncoder editableRolesTree = new TreeEncoder(service.findEditableRoles(), new LevelTreeParser());
		editableRolesTree.setNeedRootNode(false);
        
    	print(new String[]{"GroupInfo", "Group2RoleTree", "Group2RoleExistTree", "Group2UserExistTree"}, 
                new Object[]{groupEncoder, editableRolesTree, rolesTreeEncoder, usersTreeEncoder});
	}
    
    /**
     * 保存一个Group对象的明细信息、用户组对用户信息、用户组对角色的信息
     */
	@RequestMapping(method = RequestMethod.POST)
    public void saveGroup(HttpServletResponse response, HttpServletRequest request, Group group) {
    	String group2UserExistTree = request.getParameter("Group2UserExistTree");
    	String group2RoleExistTree = request.getParameter("Group2RoleExistTree");
    	
        boolean isNew = group.getId() == null;
        if ( isNew ) { // 新建
            service.createNewGroup(group, group2UserExistTree, group2RoleExistTree);
        } else { // 编辑
            service.editExistGroup(group, group2UserExistTree, group2RoleExistTree);
        }
        doAfterSave(isNew, group, "GroupTree");
    }
    
    /**
     * 启用或者停用用户组
     */
    @RequestMapping(value = "/disable/{id}/{state}", method = RequestMethod.POST)
    public void startOrStopGroup(HttpServletResponse response, 
    		@PathVariable("id") Long id, @PathVariable("state") int state) {
    	
        service.startOrStopGroup(id, state);
        printSuccessMessage();
    }
    
    /**
     * 删除用户组
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteGroup(HttpServletResponse response, @PathVariable("id") Long id) {
        service.deleteGroup(id);     
        printSuccessMessage();
    }
    
    /**
     * 用户组的排序
     */
    @RequestMapping(value = "/sort/{id}/{targetId}/{direction}")
    public void sortGroup(HttpServletResponse response,             
    		@PathVariable("id") Long id, 
            @PathVariable("targetId") Long targetId, 
            @PathVariable("direction") int direction) {
    	
        service.sortGroup(id, targetId, direction);
        printSuccessMessage();
    }  
    
	/** 移动 */
    @RequestMapping(value = "/move/{id}/{toGroupId}", method = RequestMethod.POST)
	public void move(HttpServletResponse response, 
			@PathVariable("id") Long id, 
			@PathVariable("toGroupId") Long toGroupId) {
    	
    	service.move(id, toGroupId);        
        printSuccessMessage();
	}
}
