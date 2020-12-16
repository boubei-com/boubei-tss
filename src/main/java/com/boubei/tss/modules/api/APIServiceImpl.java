package com.boubei.tss.modules.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.dml.SqlConfig;
import com.boubei.tss.dm.dml.SqlConfig.Script;
import com.boubei.tss.dm.report.log.AccessLogRecorder;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@Service("APIService")
public class APIServiceImpl implements APIService {
	
	Log log = LogFactory.getLog(this.getClass());
	
	@Autowired IUserDao userDao;
	@Autowired IRoleDao roleDao;
	
	@Autowired IUserService userService;
	@Autowired ICommonService commService;
	@Autowired ILoginService loginService;
	
	public User getUserByCode(String userCode) {
		return userDao.getUserByAccount(userCode, true);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> searchTokes(String uName, String resource, String type) {
		Date now = new Date();
		
		String hql = "select token from UserToken where user=? and resource=? and type=? and expireTime > ?";
		List<String> tokens = (List<String>) userDao.getEntities(hql, uName, resource, type, now );
		tokens.addAll( (List<String>) userDao.getEntities(hql, Anonymous._CODE, resource, type, now ) );
		
		/* 专门用于SSO的令牌，一个用户可以有多个（比如一个手机号通过多个小程序登录同一后台，每个小程序的openId都可登录）*/
		hql = "select token from UserToken where user=? and type='SSO' and expireTime > ? and (domain is null or domain not like '%@--')";
		tokens.addAll( (List<String>) userDao.getEntities(hql, uName, now) );
		
		/*
		 *  把用户的MD5密码也作为令牌，如果和uToken对的上，给予通过（适用于开放数据表链接给第三方用户录入，此时不宜逐个给用户发放令牌）
		 *  令牌校验通过后，对访问的数据服务、数据表接口等资源是否有相应的操作权限，还要在_Recorder和_Reporter里进一步校验。
		 *  自定义的接口 /api/*，需要在接口方法内，进行相应的角色和数据等控制
		 */
		User user = getUserByCode(uName);
		Object uToken = EasyUtils.checkNull(user.getAuthToken(), user.getPassword());
		tokens.add( (String) uToken );
		
		return tokens;
	}
	
	public String autoLogin(String userCode) {
		return mockLogin(userCode);
	}
	
    public String mockLogin(String userCode) {
    	return userDao.mockLogin(userCode);
    }
    

	public boolean setRole4User(Map<String,String> requestMap, String userCode, String group, String roles) {
		userCode = userCode.trim();
		group = (String) EasyUtils.checkNull(requestMap.get("group"), group); // 可能中文
		boolean ignoreExistRoles = "true".equals( requestMap.get("ignoreExistRoles") );
		User user = userService.getUserByLoginName(userCode);
		String groupStr;
		
		// 移动到目标组下
		String hql = "select id from Group where domain = ? and ? in (name,id) order by decode asc";
		List<?> list = commService.getList(hql, Environment.getDomain(), EasyUtils.checkNull(group, "noGroup__"));
		Object targetMainGroup = list.isEmpty() ? null : list.get(0);
		
		// 只能修改自己域下用户，修改别的域已存在的用户会报账号已存在 (可以指定例外的域：比如体验域的允许被其它域移走)
		String hql2 = "select gu.userId from Group g, GroupUser gu where g.id = gu.groupId and g.groupType = ? and g.domain = ?";
        List<?> testAccouts = commService.getList(hql2, Group.MAIN_GROUP_TYPE, ParamManager.getValue("TEST_DOMAIN", "G1"));
		
		Map<String, String> domainUsersMap = loginService.getUsersMap(Environment.getDomain());
		boolean if_exist = user != null && (domainUsersMap.containsKey(userCode) || testAccouts.contains(user.getId()) );
		String _roles;
		if( if_exist ) {
			Long userID = user.getId();
			
			List<Object[]> groups = loginService.getAssistGroups(userID);
			groupStr = EasyUtils.list2Str(groups, 0);
			
			// 如果指定了目标组，则移动；没有则还是在当前主用户组下
			groupStr += "," + EasyUtils.checkNull(targetMainGroup, loginService.getMainGroup(userID)[0]);
			
			// 忽略已有的角色，重新指定角色
			if( ignoreExistRoles ) { 
				_roles = roles;
			}
			// 在已有角色的基础上新加角色
			else {
				List<Long> exsitRoles = loginService.getRoleIdsByUserId(userID);
				_roles = roles + "," + EasyUtils.list2Str(exsitRoles);
			}
		} 
		else {
			user = new User();
			user.setLoginName(userCode);
			user.setPassword(userCode);
	        
			// 如果没有指定目标组，则创建用户到当前创建人所在组下
			targetMainGroup = EasyUtils.checkNull(targetMainGroup, Environment.getUserGroupId());
			groupStr = targetMainGroup + ""; 
			
			_roles = roles;
		}
		
		int disabled = EasyUtils.obj2Int(requestMap.get("disabled"));
		user.setUserName( (String) EasyUtils.checkNull(requestMap.get("userName"), user.getUserName(), user.getLoginName()) );
		user.setTelephone( (String) EasyUtils.checkNull(requestMap.get("mobile"), user.getTelephone()) );
		user.setUdf(requestMap.get("udf"));
		user.setDisabled(disabled);
		userService.createOrUpdateUser(user, groupStr, _roles);
		
		if(disabled == ParamConstants.TRUE) {
			return if_exist;
		}
		
		// 检查角色是否设置成功，没有的话抛出异常（设置人对设置角色没有编辑权限）。
		List<Long> userRoles = loginService.getRoleIdsByUserId(user.getId());
		String[] rolesArray = roles.split(",");
		for( String role : rolesArray ) {
			Long roleId = EasyUtils.obj2Long(role);
			if( roleId.longValue() != 0 && !userRoles.contains(roleId) ) {
				Role r = roleDao.getEntity(roleId);
				throw new BusinessException(EX.parse(EX.U_54, r));
			}
		}
		
		return if_exist;
	}
	
	public SQLExcutor queryByScript(String sqlCode, Map<String, String> params, int maxPagesize, String tag, Object cacheFlag) {
		String sqlPath  = params.get("sqlPath");
		Object page     = params.get("page");
		Object pagesize = params.get("rows");
		int _pagesize = EasyUtils.obj2Int(EasyUtils.checkNull(pagesize, maxPagesize));
		int _page = page != null ? EasyUtils.obj2Int(page) : 1;
		long start = System.currentTimeMillis();
		
//		com.boubei.tss.cache.extension.CacheHelper.getNoDeadCache().destroyByKey( "SQL_CONFIG_INIT_SCRIPT" );
		
		Script script = SqlConfig._getScript(sqlCode, sqlPath);
		String sql = script.sql;
		String datasource = (String) EasyUtils.checkNull(script.ds, DMConstants.LOCAL_CONN_POOL);
		String bIDataProcessImpl = (String) EasyUtils.checkNull(script.dataProcess, BIDataProcess.class.getName());
		
		String entityFilter = (String) EasyUtils.checkNull(params.get("entityFilter"), script.defaultFilter);
		if (!EasyUtils.isNullOrEmpty(entityFilter)) {
			params.put("entityFilter", entityFilter);
		}
		
		BIDataProcess biDataProcess = (BIDataProcess) BeanUtil.newInstanceByName(bIDataProcessImpl);
		// 处理查询前的事情
		sql = biDataProcess.beforeHandleSql(sql, params);
		sql = biDataProcess.handleSql(sql, params);
		
		
		Map<String, String> fmParams = new HashMap<String, String>();
		for (Entry<String, ?> entry : params.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			if (val != null && !"".equals(val)) {
				String val_ = (String) val;
				if ( Pattern.compile("in[\\s]*\\(\\$\\{[\\s]*(" +key+ ")[\\s]*\\}\\)").matcher(sql).find() ) {
					val_ = DMUtil.insertSingleQuotes(val_); 
				}
				fmParams.put(key, val_);
			}
		}
		
		sql = DMUtil.fmParse(sql, fmParams);

		SQLExcutor ex = new SQLExcutor();
		ex.testFlag = params.containsKey("testFlag");
		ex.excuteQuery(sql, null, _page, _pagesize, datasource);
		
		// 处理查询出来的数据
		biDataProcess.handle(ex, params, tag);
		
		if( script.recordLog || System.currentTimeMillis() - start > 1000 ) {
			AccessLogRecorder.outputAccessLog("/bi/sql", sqlCode, tag, params, start);
		}
		
		return ex;
	}
}
