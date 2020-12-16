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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.permission.RoleUserMapping;
import com.boubei.tss.um.entity.permission.RoleUserMappingId;
import com.boubei.tss.um.helper.PasswordRule;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.InfoEncoder;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.MathUtil;

/**
 * <p>
 * 用户登录系统相关业务逻辑处理接口：
 * <li>根据用户登录名获取用户名及认证方式信息等；
 * <li>根据用户ID获取用户信息；
 * <li>根据用户登录名获取用户信息；
 * </p>
 */
@Service("LoginService")
@SuppressWarnings("unchecked")
public class LoginService implements ILoginService {
	
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired private IUserDao userDao;
	@Autowired private IGroupDao groupDao;
	@Autowired private IRoleDao roleDao;
	
	public int checkPwdErrorCount(String loginName) {
		User user = getUserByLoginName(loginName);
		int count = user.getPwdErrorCount();
	    
	    // 离最后一次输错密码已经超过十分钟了，则统计次数重新清零
		Date lastPwdErrorTime = user.getLastPwdErrorTime();
	    if(lastPwdErrorTime == null 
	    		|| System.currentTimeMillis() - lastPwdErrorTime.getTime() > 10*60*1000) {
	    	count = 0;
	    }
	    
		if( count >= 10) {
			throw new BusinessException(EX.U_25);
		}
		return count;
	}
	
	public void recordPwdErrorCount(String loginName, int currCount) {
		User user = getUserByLoginName(loginName);

		currCount ++;
	    if(currCount >= 5) {
	    	log.info("【" + loginName + "】已连续【" +currCount+ "】次输错密码。IP=" + Environment.getClientIp());
	    }
	    
	    user.setLastPwdErrorTime(new Date());
    	user.setPwdErrorCount(currCount);
    	userDao.refreshEntity(user);
	}
	
	public Object resetPassword(Long userId, String passwd) {
		User user = userDao.getEntity(userId);
		if( UMConstants.ADMIN_USER_ID.equals(userId) && !Environment.isAdmin() ) return user;
		
		String token = InfoEncoder.simpleEncode(userId.toString(), MathUtil.randomInt(12));
    	user.setOrignPassword( passwd );
    	
    	if(Context.isOnline()) {
    		IOperator operator = Context.getIdentityCard().getOperator();
    		operator.getAttributesMap().put("passwordStrength", user.getPasswordStrength());
    	}
    	
    	userDao.refreshEntity(user);
    	
    	// 刷新cache，否则密码修改后要等10分钟才生效
    	CacheHelper.flushCache(CacheLife.SHORT.toString(), "ByLoginName(" +user.getLoginName()+ ")");
    	return token;
	}

	// For登录用, 此处调用会校验User的状态，停用、过期等状态会抛出异常
	public User getLoginInfoByLoginName(String loginName) {
		return getUserByLoginName(loginName);
	}
	
	private User getUserByLoginName(String loginName) {
        User user = userDao.getUserByAccount(loginName, true);
        userDao.evict(user);
        return user;
	}
	
	/* 检查用户的密码强度，太弱的话强制要求修改密码。在用户登录成功访问的第一个页面前在Filter0Security触发 */
	public int checkPwdSecurity(Long userId) {
    	Object strengthLevel = null;
    	try {
			IOperator operator = getOperatorDTOByID(userId);
			strengthLevel = operator.getAttributesMap().get("passwordStrength");
    	} 
    	catch(Exception e) { }
    	
    	if(EasyUtils.obj2Int(strengthLevel) <= PasswordRule.LOW_LEVEL ) {
    		return 0;
		}
    	
    	// 检查用户上次修改密码时间，如果超过了180天，则将安全等级降低为0
		User user = userDao.getEntity(userId);
		Date lastPwdChangeTime = user.getLastPwdChangeTime();
		int passwdCyclelife = EasyUtils.obj2Int(ParamManager.getValue("passwd.cyclelife", "180"));
		lastPwdChangeTime = (Date) EasyUtils.checkNull(lastPwdChangeTime, user.getLastLogonTime(), new Date());
		if( DateUtil.addDays(lastPwdChangeTime, passwdCyclelife).before( new Date() ) ) {
			return -1;
		}
    	
    	return 1;
	}

	public OperatorDTO getOperatorDTOByID(Long userId) {
		User user = userDao.getEntity(userId);
		return new OperatorDTO(user);
	}

	public OperatorDTO getOperatorDTOByLoginName(String loginName) {
	    User user = getUserByLoginName(loginName);
	    return new OperatorDTO(user);
	}
    
    public List<Long> saveUserRoleMapping(Long logonUserId) {
    	List<Long> roleIds = getRoleIdsByUserId( logonUserId );
        return saveUserRoleMapping(logonUserId, roleIds);
	}
    
    public List<Long> saveUserRoleMapping(Long logonUserId, List<Long> roleIds) {
		List<?> exsits = userDao.getEntities("from RoleUserMapping where id.userId = ?", logonUserId);
    	Map<Long, RoleUserMapping> history = new HashMap<Long, RoleUserMapping>();
    	for(Object obj : exsits) {
    		RoleUserMapping ru = (RoleUserMapping) obj;
    		history.put(ru.getId().getRoleId(), ru);
    	}
        
        // 默认插入一条【匿名角色】给每一个登录用户
    	if( !roleIds.contains(UMConstants.ANONYMOUS_ROLE_ID) ) {
    		roleIds.add(0, UMConstants.ANONYMOUS_ROLE_ID );
    	}
        Set<Long> roleSet = new HashSet<Long>( roleIds ); // 去重
        
        for(Long roleId : roleSet) {
        	if( history.remove(roleId) == null ) {
        		RoleUserMappingId id = new RoleUserMappingId();
    			id.setUserId(logonUserId);
    			id.setRoleId(roleId);
    			
    			RoleUserMapping entity = new RoleUserMapping();
    			entity.setId(id);
    			
    			userDao.createObject(entity);  // 新增，多线程不安全
        	}
		}
        
        // 删除之前有，现在没了的角色（可能存在重复的ru，用 userDao.deleteAll( history.values() ) 会删不干净）
        for( RoleUserMapping ru : history.values() ) {
        	RoleUserMappingId ruId = ru.getId();
			userDao.executeHQL("delete from RoleUserMapping where userId = ? and roleId = ?", ruId.getUserId(), ruId.getRoleId());
        }
        
        return roleIds;
	}

	public List<Long> getRoleIdsByUserId(Long userId) {
		String hql = "select distinct o.id.roleId from ViewRoleUser o where o.id.userId = ? order by o.id.roleId ";
        return (List<Long>) userDao.getEntities(hql, userId);
    }
    
    public List<String> getRoleNames(Collection<Long> roleIds) {
    	if( roleIds.isEmpty() ) {
    		return new ArrayList<String>();
    	}
    	List<?> names = roleDao.getEntities("select name from Role where id in (" +EasyUtils.list2Str(roleIds)+ ") order by id");
        return (List<String>) names;
    }
    
	public List<Object[]> getAssistGroups(Long userId) {
		return getUserGroups(userId, Group.ASSISTANT_GROUP_TYPE);
	}
	
	public Object[] getMainGroup(Long userId) {
        return getUserGroups(userId, Group.MAIN_GROUP_TYPE).get(0);
	}
	
	private List<Object[]> getUserGroups(Long userId, Integer groupType) {
        String hql = "select distinct g.id, g.name, g.domain from Group g, GroupUser gu " +
        		" where g.id = gu.groupId and gu.userId = ? and g.groupType = ?";
        return (List<Object[]>) userDao.getEntities(hql, userId, groupType);
	}

	public List<Group> getGroupsByUserId(Long userId) {
		List<?> list = groupDao.getFatherGroupsByUserId(userId);
		List<Group> result = new ArrayList<Group>();
		for (int i = 1; i < list.size() + 1; i++) {
			Group group = (Group) list.get(i - 1);
			result.add(group);
		}
		return result;
	}

    public List<GroupDTO> getGroupTreeByGroupId(Long groupId) {
        List<Group> groups = groupDao.getChildrenById(groupId);
        
        List<GroupDTO> returnList = new ArrayList<GroupDTO>();
        for( Group group : groups ){
            GroupDTO dto = new GroupDTO();
            dto.setId(group.getId().toString());
            dto.setName(group.getName());
            dto.setParentId(group.getParentId().toString());
            returnList.add(dto);
        }
        
        return returnList;
    }
    
    public List<OperatorDTO> getUsersByGroupId(Long groupId, boolean deeply) {
        List<User> users;
        if(deeply) {
        	users = groupDao.getUsersByGroupIdDeeply(groupId);
        } else {
        	users = groupDao.getUsersByGroupId(groupId);
        }
        return translateUserList2DTO(users);
    }
    
    // 如果domain为空，则取主用户组下非域组用户
    public List<OperatorDTO> getUsersByRoleId(Long roleId) {
    	String domain = Environment.getDomainOrign();
    	String domainCondition = (String) EasyUtils.checkTrue(EasyUtils.isNullOrEmpty(domain), "and g.domain is null", "and g.domain = '"+ domain + "'");
    	return _getDomainUsersByRole(roleId, domainCondition);
    }
    
    // 如果domain为空，则忽略域查询条件
    public List<OperatorDTO> getUsersByRoleId(Long roleId, String domain) {
    	String domainCondition = (String) EasyUtils.checkTrue(EasyUtils.isNullOrEmpty(domain), "", "and g.domain = '"+ domain + "'");
    	return _getDomainUsersByRole(roleId, domainCondition);
    }
 
    private List<OperatorDTO> _getDomainUsersByRole(Long roleId, String domainCondition) {
        String hql = "select distinct u, g.decode from ViewRoleUser ru, User u, GroupUser gu, Group g" +
                " where ru.id.userId = u.id and ru.id.roleId = ? " +
                " 	and u.id = gu.userId and gu.groupId = g.id and g.groupType = 1 " + domainCondition + 
                " order by g.decode desc, u.id asc ";
        
		List<?> data = (List<User>) groupDao.getEntities( hql, roleId);
		List<User> users = new ArrayList<User>();
		for(Object obj : data) {
			users.add((User) ((Object[])obj)[0]);
		}
		
        return translateUserList2DTO(users);
    }
    
    public List<OperatorDTO> getUsersByRole(String roleName, String domain) {
    	List<?> roles = roleDao.getEntities("from Role where name = ? and isGroup = 0", roleName);
    	if( roles.isEmpty()) {
    		throw new BusinessException("角色：" + roleName + " 不存在");
    	}
		Long roleId = ((Role) roles.get(0)).getId();
		return getUsersByRoleId(roleId, domain);
    }
    
    public List<?> getUsersByDomain(String domain, String field, Long logonUserId) {
    	domain = (String) EasyUtils.checkNull(domain, UMConstants.DEFAULT_DOMAIN);
    	String selfRegDomain = groupDao.getEntity(UMConstants.SELF_REGISTER_GROUP_ID).getDomain();
    	String devDomain = groupDao.getEntity(UMConstants.DEV_GROUP_ID).getDomain();
    	
    	// 如果当前用户属于开发者域或自注册域，则只返回自己个人账号
    	if(domain.equals(selfRegDomain) || domain.equals(devDomain)) {
    		return userDao.getEntities( "select distinct u." +field+ " from User u where u.id=?", logonUserId );
    	}
    	
        String hql = "select distinct u." +field+ " from Group g, GroupUser gu, User u" +
                " where gu.userId = u.id and gu.groupId = g.id and groupType = 1 " +
                "	and ? in (g.domain, '无域') " +
                " order by u." + field;
       
        return userDao.getEntities( hql, domain );
    }
    
    // 登陆账号和中文名字映射
    public Map<String, String> getUsersMap(String domain) {
		return (Map<String, String>) _getUsersMap(domain, "loginName, u.userName");
	}
    
    // 登陆账号ID和中文名字映射
    public Map<Long, String> getUsersMapI(String domain) {
		return (Map<Long, String>) _getUsersMap(domain, "id, u.userName");
	}
    
    private Map<?, String> _getUsersMap(String domain, String key) {
		List<?> list = getUsersByDomain(domain, key, Environment.getUserId());
		
		Map<Object, String> map = new HashMap<Object, String>();
		for( Object obj : list ) {
			Object[] objs = (Object[]) obj;
			map.put(objs[0], (String)objs[1]);
		}
		return map;
	}
    
    private List<OperatorDTO> translateUserList2DTO(List<User> users){
        List<OperatorDTO> returnList = new ArrayList<OperatorDTO>();
        for( User user : users ){
            OperatorDTO dto = new OperatorDTO(user);
            dto.setPassword(null);
			returnList.add(dto);
        }
        return returnList;
    }
    
	public String[] getContactInfos(String receiverStr, boolean justID) {
    	if(receiverStr == null) return null;
    	
    	Map<String, Object> fmDataMap = new HashMap<String, Object>();
		List<Param> macroParams = ParamManager.getComboParam(PX.EMAIL_MACRO);
		macroParams = (List<Param>) EasyUtils.checkNull(macroParams, new ArrayList<Param>());
		for(Param p : macroParams) {
			fmDataMap.put(p.getText(), p.getValue());
		}
		
		receiverStr = MacrocodeCompiler.runLoop(receiverStr, fmDataMap, true);
		String[] receiver = receiverStr.split(",");
		
		// 将登陆账号转换成该用户的邮箱
		Set<String> emails = new HashSet<String>();
		Set<Long> ids = new HashSet<Long>();
		for(int j = 0; j < receiver.length; j++) {
			String temp = receiver[j];
			
			// 判断配置的是否已经是email，如不是，作为loginName处理
			int index = temp.indexOf("@tssRole");
			if(index > 0) { // 角色
				String domain = temp.substring(index + 8).replaceFirst("@", "");
				List<OperatorDTO> list = getUsersByRoleId(parseID(temp), domain); // 按角色ID
				try {
					list.addAll( getUsersByRole(temp.split("@")[0], domain) ); // 按角色名字
				} catch(Exception e) { }
				
				for(OperatorDTO user : list) {
					addUserEmail2List(user, emails, ids);
				}
			} 
			else if(temp.endsWith("@tssGroup")) { // 用户组
				List<OperatorDTO> list = getUsersByGroupId(parseID(temp), false);
				for(OperatorDTO user : list) {
					addUserEmail2List(user, emails, ids);
				}
			} 
			else if(temp.endsWith("@tssGroupDeep")) { // 用户组（含子组）
				List<OperatorDTO> list = getUsersByGroupId(parseID(temp), true);
				for(OperatorDTO user : list) {
					addUserEmail2List(user, emails, ids);
				}
			}
			else if(temp.indexOf("@") < 0) { // LoginName
				try {
					OperatorDTO user = getOperatorDTOByLoginName(temp);
					addUserEmail2List(user, emails, ids);
				} 
				catch(Exception e) {
				}
			}
			else if(temp.indexOf("@") > 0 && temp.indexOf(".") > 0) { // email地址
				emails.add(temp);
			}
		}
		
		if(justID) {
			return ids.isEmpty() ? new String[]{} : EasyUtils.list2Str(ids).split(",");
		}
		
		receiver = new String[emails.size()];
		receiver = emails.toArray(receiver);
		
		return receiver;
	}
	
	private Long parseID(String temp) {
		try {
			return EasyUtils.obj2Long( temp.split("@")[0] );
		} catch(Exception e) {
			return 0L;
		}
	}
 
	private void addUserEmail2List(OperatorDTO user, Set<String> emails, Set<Long> ids) {
		String email = (String) user.getAttribute("email");
		if( !EasyUtils.isNullOrEmpty(email) ) {
			emails.add( email );
		}
		ids.add(user.getId());
	}
}