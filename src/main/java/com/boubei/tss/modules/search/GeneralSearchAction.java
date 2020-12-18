/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;
import com.boubei.tss.framework.web.display.tree.DefaultTreeNode;
import com.boubei.tss.framework.web.display.tree.ITreeNode;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.StringUtil;

/**
 *  安全性控制: 按组、角色、域获取用户信息时，必须对组、角色、域有查看权限
 */
@Controller
@RequestMapping( {"/auth/search", "/auth/service", "/api/service"} )
public class GeneralSearchAction extends BaseActionSupport {
	
	@Autowired private GeneralSearchService service;
	@Autowired private IRoleService roleService;
	@Autowired private IGroupService groupService;
	@Autowired private ICommonService commonService;
	@Autowired private ILoginService loginService;
	
	/**
	 * 检索数据表、报表、菜单、用户组等，带权限过滤。打开资源时再单独过滤权限。
	 * TODO Group 还需要过滤用户域;
	 */
	@RequestMapping("/resource")
    @ResponseBody
	public List<?> searchResource(String resource, String key) {
		
		key = "%" +key+ "%";
		String condition = "o.name like ?1";
		
		// record、report检索需要更多的关键字
		if( "Report".equals(resource) ) { 
			condition = "(o.name like ?1 or o.param like '" +key+ "' or o.remark like '" +key+ "') and o.name not like '$%' and o.type=1 ";
		}
		if( "Record".equals(resource) ) { 
			condition = "(o.name||o.define like ?1 or o.remark like '" +key+ "') and o.name not like '$%' and o.type=1 ";
		}
		
		String permissionTable = resource + "Permission";
		String hql = "select distinct o.id, o.name, o.decode from " +resource+ " o, " +permissionTable+ " p " +
				" where " +condition+ " and disabled = 0 " +
				"   and p.resourceId = o.id and p.roleId in (" +Environment.getInSession(SSOConstants.USER_RIGHTS_S)+ ") ";
		String hql2 = "select name from " +resource+ " o where ?1 like o.decode||'%' and id <> ?2 order by o.decode";
		
		List<?> result = commonService.getList(hql, key);
		for(Object obj : result) {
			Object[] objs = (Object[]) obj;
			List<?> parents = commonService.getList(hql2, objs[2], objs[0]);
			objs[2] = EasyUtils.list2Str(parents, " > ");
		}
		
		return result;
	}
 
	/**
	 * 一个组下面所有用户的因转授而获得的角色的情况
	 */
	@RequestMapping("/subauth/{groupId}")
	public void searchUserSubauth(HttpServletResponse response, @PathVariable("groupId") Long groupId) {
		List<?> list = service.searchUserSubauthByGroupId(groupId);
		GridDataEncoder gridEncoder = new GridDataEncoder(list, UMConstants.GENERAL_SEARCH_STRATEGY_GRID);
				
        print("SUBAUTH_RESULT", gridEncoder);
	}
	
	/**
	 * 根据用户组查询组下用户（需是登陆用户可见的用户）的角色授予情况
	 */
	@RequestMapping("/roles/{groupId}")
	public void searchRolesByGroup(HttpServletResponse response, @PathVariable("groupId") Long groupId) {
		List<?> list = service.searchUserRolesMapping(groupId);
		GridDataEncoder gridEncoder = new GridDataEncoder(list, UMConstants.GENERAL_SEARCH_ROLE_GRID);

		print("ROLE_RESULT", gridEncoder);
	}
	
	/**
	 * 拥有同一个角色的所有用户列表
	 */
	@RequestMapping("/role/users/{roleId}")
	public void searchUsersByRole(HttpServletResponse response, @PathVariable("roleId") Long roleId) {
		List<?> list = service.searchUsersByRole(roleId);
		GridDataEncoder gridEncoder = new GridDataEncoder(list, UMConstants.GENERAL_SEARCH_USER_GRID);

        print("ROlE_USERS_RESULT", gridEncoder);
	}
	
	/**
	 * 获取当前用户有【查看权限】的角色，用于前台生成角色下拉列表
	 */
	@RequestMapping(value = "/roles", method = RequestMethod.GET)
	@ResponseBody
	public List<Object[]> getVisiableRoles() {
		List<?> list = roleService.getAllVisiableRole();
		List<Object[]> returnList = new ArrayList<Object[]>();
		for(Object temp : list) {
			Role role = (Role) temp;
			boolean isRole = ParamConstants.FALSE.equals(role.getIsGroup());
			boolean enable = ParamConstants.FALSE.equals(role.getDisabled());
			if( isRole && enable && role.getId() > 0 ) {
				returnList.add(new Object[]{ role.getId(), role.getName() });
			}
		}
		
		returnList.add(new Object[]{ UMConstants.ANONYMOUS_ROLE_ID, UMConstants.ANONYMOUS_ROLE });
		return returnList;
	}
	
	@RequestMapping(value = "/roles2", method = RequestMethod.GET)
	@ResponseBody
	public List<Object> getEditableRoles() {
		List<Role> list = groupService.findEditableRoles();;
		List<Object> returnList = new ArrayList<Object>();
		for(Role role : list) {
			boolean isRole = ParamConstants.FALSE.equals(role.getIsGroup());
			boolean enable = ParamConstants.FALSE.equals(role.getDisabled());
			if( isRole && enable && role.getId() > 0 ) {
				Map<String, Object> m = new HashMap<>();
				m.put("id", role.getId());
				m.put("name", role.getName());
				returnList.add(m);
			}
		}
		
		return returnList;
	}
	
	@RequestMapping("/roles/tree")
    public void getVisiableRolesTree(HttpServletResponse response) {
        List<Object[]> roles = this.getVisiableRoles();
        List<ITreeNode> nodes = new ArrayList<ITreeNode>();
        for(Object[] role : roles) {
        	nodes.add( new DefaultTreeNode(role[0], (String) role[1]) );
        }
        
        TreeEncoder treeEncoder = new TreeEncoder(nodes);
        treeEncoder.setNeedRootNode(false);
        print("RoleTree", treeEncoder);
    }
	
	/**
	 * https://www.boudata.com/tss/auth/service/rusers/-1
	 * 可直接用作下拉列表，返回值里有 text/value 属性
	 */
	@RequestMapping(value = "/rusers/{roleId}", method = RequestMethod.GET)
	@ResponseBody
	public List<Object> getUsersByRoleId(@PathVariable Long roleId) {
		List<User> list = service.searchUsersByRole(roleId);
		List<User> list2 = new ArrayList<User>();
		
		for(User user : list) {
			user = (User) commonService.getEntity(User.class, user.getId());
			list2.add(user);
		}
		return buildUserList(list2);
	}
	
	/**
	 * https://www.boudata.com/tss/auth/service/gusers/-8
	 * 可直接用作下拉列表，返回值里有 text/value 属性
	 */
	@RequestMapping(value = "/gusers/{groupId}", method = RequestMethod.GET)
	@ResponseBody
	public List<Object> getUsersByGroupId(@PathVariable Long groupId) {
		List<User> list = groupService.getUsersByGroupId(groupId);
		return buildUserList(list);
	}
	
	@RequestMapping(value = "/domainuser", method = RequestMethod.GET)
	@ResponseBody
	public List<?> getUsersByDomain(String domain, String field) {
		if( !Environment.isAdmin() && !Environment.getDomain().equals(domain) ) {
			return new ArrayList<String>();
		}
		
		field = field.replaceAll("password", "").replaceAll("authToken", ""); // 禁止查询敏感字段
		return loginService.getUsersByDomain(domain, field, -0L);
	}
	
	@RequestMapping(value = "/domaingroups")
	@ResponseBody
	public List<?> getDomainGroups() {
		String sql = "select id, name, parentId as pid from um_group g where g.domain = ? order by decode";
		List<Map<String, Object>> list = SQLExcutor.queryL(sql, Environment.getDomain());
		Map<Object, Object> m = new HashMap<Object, Object>();
		int index = 0;
		for(Map<String, Object> row : list) {
			Object id = row.get("id");
			if( index++ == 0 ) {
				row.put("name", "-");
			}
			Object name = row.get("name");
			m.put(id, name);
			row.put("pname", m.get( row.get("pid") ));
			row.put("text", EasyUtils.obj2String(row.get("pname")) + "/" + name );
			row.put("value", id);
		}
		return list;
	}
	
	@RequestMapping(value = "/domains", method = RequestMethod.GET)
	@ResponseBody
	public List<?> getDomains() {
		List<Group> result = new ArrayList<Group>();
		List<?> groups = groupService.getVisibleSubGroups(UMConstants.MAIN_GROUP_ID);
		for(Object temp : groups) {
			Group g = (Group) temp;
			if( g.isDomainGroup() ) {
				result.add(g);
			}
		}
		return result;
	}
	
	@RequestMapping("/rid")
	@ResponseBody
	public Long getRoleId(String role) {
		List<?> roles = commonService.getList(" select id from Role where name=?1 order by id desc", role);
		return (Long) (roles.isEmpty() ? null : roles.get(0));
	}

	private List<Object> buildUserList(List<User> list) {
		List<Object> returnList = new ArrayList<Object>();
		
		for(User user : list) {
			Map<String, Object> map = user.getAttributes4XForm();
			map.remove("password");
			map.remove("passwordQuestion");
			map.remove("passwordAnswer");
			map.remove("authMethod");
			map.remove("authToken");
			
			// 用于制作用户下拉列表（Easyui、tssJS的combobox）
			String group = user.getGroupName();
			String text = user.getUserName() + EasyUtils.checkTrue(group == null , "" , "(" +group+ ")" );
			String value = user.getLoginName();
			map.put("text", text);
			map.put("value", value);
			map.put("name", text);
			map.put("code", value);
			
			returnList.add(map);
		}
		return returnList;
	}
	
	/**
	 * 获取自己所在组下的用户及父组的用户
	 */
	@RequestMapping(value = "/domain/role/users")
	@ResponseBody
	public List<?> getDomainUsersByRole(String role) {
		String[] roles = StringUtil.split(role);
		List<OperatorDTO> temp = new ArrayList<>();
		for( String _role : roles ) {
			if( EasyUtils.isDigit(_role) ) {
				temp.addAll( loginService.getUsersByRoleId(EasyUtils.obj2Long(_role), Environment.getDomain()) );
			}
			else {
				temp.addAll( loginService.getUsersByRole(_role, Environment.getDomain()) );
			}
		}
		return temp;
	}
}