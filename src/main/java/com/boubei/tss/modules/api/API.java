package com.boubei.tss.modules.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.menu.INavigatorService;
import com.boubei.tss.modules.menu.MenuDTO;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IMessageService;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.util.EasyUtils;

/**
 * 远程调用接口。
 * GeneralSearchAction 无法被远程访问
 */
@Controller
@RequestMapping( {"/api", "/auth/api"} )
public class API {
	
	@Autowired ILoginService loginService;
	@Autowired INavigatorService menuService;
	@Autowired IRoleService roleService;
	@Autowired ICommonService commService;
	@Autowired APIService apiService;
	@Autowired IMessageService msgService;
	
	@RequestMapping("/message/level2")
	@ResponseBody
    public List<?> hignLevelMessagegs() {
        return msgService.getUnReadHignLevelMsg(7);
    }
	
	@RequestMapping(value = "/message/read/{ids}", method = RequestMethod.POST)
    @ResponseBody
    public String batchRead(@PathVariable("ids") String ids) {
		msgService.batchRead(ids);
    	return "success";
    }
	
	@RequestMapping("/menu/json/{id}")
	@ResponseBody
    public List<MenuDTO> menuJSON(@PathVariable("id") Long id) {
        return menuService.getMenuTree(id);
    }
	
	@RequestMapping("/menu/json")
	@ResponseBody
    public List<MenuDTO> menuJSON(HttpServletRequest request) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		String menuName = requestMap.get("name");
		Object id = SQLExcutor.queryVL("select id from portal_navigator where name = ?", "id", menuName);
        return menuService.getMenuTree( EasyUtils.obj2Long(id) );
    }
	
	/**
	 * http://localhost:9000/tss/api/roles
	 * 
	 * 系统角色列表，用于解析角色名对角色ID。 
	 * 例：staff_info.position记录的是角色ID，判断是否包含某角色时不宜写死角色ID，通过名称先去找出角色ID
	 * {"业务员":73, 73:"业务员","人事经理":69,69:"人事经理"....}
	 */
	@RequestMapping(value = "/roles")
	@ResponseBody
	public Map<Object, Object> getRoles() {
		Map<Object, Object> roleMap = new HashMap<Object, Object>();
		
		String sql = "select id, name from um_role where isGroup=0 and disabled=0 and id > 0 ";
		List<Map<String, Object>> list = SQLExcutor.queryL(sql);
		for( Map<String, Object> m : list ) {
			Object roleName = m.get("name");
			Object roleId = m.get("id");
			
			roleMap.put(roleName, roleId);
			roleMap.put(roleId, roleName);
		}
		return roleMap;
	}
	
	/**
	 * 获取当前用户有【查看权限】的角色，用于前台生成角色下拉列表
	 */
	@RequestMapping(value = "/roleList", method = RequestMethod.GET)
	@ResponseBody
	public List<?> getVisiableRoles() {
		List<?> list = roleService.getAllVisiableRole();
		List<Role> returnList = new ArrayList<Role>();
		for(Object temp : list) {
			Role role = (Role) temp;
			boolean isRole = ParamConstants.FALSE.equals(role.getIsGroup());
			boolean enable = ParamConstants.FALSE.equals(role.getDisabled());
			if( isRole && enable && role.getId() > 0 ) {
				returnList.add(role);
			}
		}
		
		Role role = new Role();
		role.setId(UMConstants.ANONYMOUS_ROLE_ID);
		role.setName(UMConstants.ANONYMOUS_ROLE);
		returnList.add( role );
		
		return returnList;
	}
	
	/**
	 * http://localhost:9000/tss/api/users
	 * 登陆账号和中文名字映射
	 */
	@RequestMapping(value = "/users")
	@ResponseBody
	public Map<String, String> getUsers() {
		return loginService.getUsersMap(Environment.getDomain());
	}
	
	@RequestMapping(value = "/userattr")
	@ResponseBody
	public Object getSessionAttr(String attr) {
		Object v1 = Environment.getInSession(attr);
		Object v2 = Environment.getDomainInfo(attr);
		Object v3 = Environment.getUserInfo(attr);
		return EasyUtils.checkNull(v1, v2, v3);
	}
	
	/**
	 * http://localhost:9000/tss/api/log
	 * 
	 * var params = {table: 'xx', code: 'xx', content: 'xx', udf1: 'xx'};
	 * tssJS.post("/tss/api/log", params);
	 * 记录日志
	 */
	@RequestMapping(value = "/log", method = RequestMethod.POST)
	@ResponseBody
	public void createLog(HttpServletRequest request) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		String table = requestMap.get("table");
		String code = requestMap.get("code");
		String content = requestMap.get("content");
		String udf1 = requestMap.get("udf1");
		BusinessLogger.log(table, code, content, udf1, System.currentTimeMillis());
	}
	
	/** 
	 * 在指定组下创建特定岗位的人员, 用于特定的页面添加账号及角色：客户管理、司机管理等；
	 * 如果人员已经存在，则修改其 组织 和 角色；
	 * 如果不存在，则新建一个用户账号；
	 */
	@RequestMapping(value = "/setUserRole", method = RequestMethod.POST)
	@ResponseBody
	public boolean setRole4User(HttpServletRequest request, String userCode, String group, String roles) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);
		return apiService.setRole4User(requestMap, userCode, group, roles);
	}
}
