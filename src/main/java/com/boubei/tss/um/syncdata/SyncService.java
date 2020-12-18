/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.Progressable;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.UserDTO;
import com.boubei.tss.um.permission.ResourcePermission;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.XMLDocUtil;
 
/**
 * TODO
 * 1、如果用户在fromApp中更换的部门，同步时无法自动为其更换部门
 *
 */
@Service("SyncService")
public class SyncService implements ISyncService, Progressable {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired private IUserDao  userDao;
    @Autowired private IGroupDao  groupDao;
    @Autowired private ResourcePermission resourcePermission;

    private Map<String, String> initParam(String paramDescXML){
        Map<String, String> param = new HashMap<String, String>();    
        Document doc = XMLDocUtil.dataXml2Doc(paramDescXML);
        for (Iterator<?> it = doc.getRootElement().elementIterator(); it.hasNext();) {
            Element element = (Element) it.next();
            param.put(element.getName(), element.getTextTrim());
        }
        return param;
    }
    
    public Map<String, Object> getCompleteSyncGroupData(Long mainGroupId) {
        // 保存UM用户组对其它应用用户组 的 ID对应的关系 key:fromGroupId -- value:mainGgroupId
        Map<String, Long> idMapping = new HashMap<String, Long>();
        
        // 取已经同步的用户组. 设置父子节点关系时用到（其实只需'同步节点'的父节点 ＋ 子枝）
        List<?> allGroups = groupDao.getEntitiesByNativeSql("select t.* from um_group t ", Group.class); 
        for(Iterator<?> it = allGroups.iterator();it.hasNext();){
            Group group = (Group)it.next();
            Long groupId = group.getId();
            String fromGroupId = group.getFromGroupId();
			fromGroupId = EasyUtils.checkNull(fromGroupId, groupId).toString(); // 没有fromGroupId，则取自己
			idMapping.put(fromGroupId, groupId);
        }

        Group mainGroup = groupDao.getEntity(mainGroupId);
        Map<String, String> syncConfig = initParam(mainGroup.getSyncConfig());
        String dsType = syncConfig.get("dsType");
        String fromGroupId = syncConfig.get("fromGroupId");
        fromGroupId = EasyUtils.obj2String( EasyUtils.checkNull(fromGroupId, mainGroup.getFromGroupId()) );
        
        List<?> groups = getGroups(dsType, syncConfig, fromGroupId); //从其它系统获取需要同步的所有用户组
        List<?> users  = getUsers (dsType, syncConfig, fromGroupId); //从其它系统获取需要同步的所有用户
        
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("groupId", mainGroupId);
        paramsMap.put("groups", groups);
        paramsMap.put("users", users);
        paramsMap.put("idMapping", idMapping);

        return paramsMap;
    }
    
    private List<?> getGroups(String dsType, Map<String, String> appParams, String groupId){
        String sql = appParams.get(SyncDataHelper.QUERY_GROUP_SQL_NAME);
        return SyncDataHelper.getOutDataDao(dsType).getOtherGroups(appParams, sql, groupId);
    }

    private List<?> getUsers(String dsType, Map<String, String> appParams, String groupId){
        String sql = appParams.get(SyncDataHelper.QUERY_USER_SQL_NAME);
        return SyncDataHelper.getOutDataDao(dsType).getOtherUsers( appParams, sql, groupId );
    }
    
    @SuppressWarnings("unchecked")
	public String execute(Map<String, Object> paramsMap, Progress progress) {
    	Long mainGroupId = (Long) paramsMap.get("groupId");
        List<?> groups = (List<?>)paramsMap.get("groups");
        List<?> users  = (List<?>)paramsMap.get("users");
        Map<String, Long> idMapping = (Map<String, Long>)paramsMap.get("idMapping");
        
        String domain = groupDao.getEntity(mainGroupId).getDomain();
        
        syncGroups(groups, idMapping, progress, domain);
        syncUsers (users, idMapping, progress);
        
        // 刷新缓存
        CacheHelper.flushCache(CacheLife.SHORTER.toString(), "getUsers");
        
        return "group num = " + groups.size() + ", user num = " + users.size();
    }

    private void syncGroups(List<?> outGroups, Map<String, Long> idMapping, Progress progress, String domain) {
        for (int i = 0; i < outGroups.size(); i++) {
            GroupDTO outGroup = (GroupDTO) outGroups.get(i);
            String fromGroupId = outGroup.getId();
            String name = outGroup.getName();
            
            Group group;
            Long parentId = idMapping.get(outGroup.getParentId()); // 获取其它应用组的父组对应UM中组的ID
            parentId = (Long) EasyUtils.checkNull(parentId, UMConstants.MAIN_GROUP_ID);
            
            // 检查组（和该fromApp下的组对应的组）是否已经存在
            List<?> temp = groupDao.getEntities("from Group t where t.fromGroupId=?1 ", fromGroupId);
            if( EasyUtils.isNullOrEmpty(temp) ) {
        		/* 
        		 * 有可能管理员已经在tss里手动创建了同名的分组，这样情况下同步组过来会违反组名唯一性约束 
        		 * 先找出同名的Group, 为其设置fromApp和fromGroup
        		 */
        		temp = groupDao.getEntities("from Group t where t.parentId=?1 and t.name=?2", parentId, name);
        		if( !EasyUtils.isNullOrEmpty(temp) ) {
        			group = (Group) temp.get(0);
        		} 
        		else {
					group = new Group();
					group.setName(  (String) EasyUtils.checkNull(outGroup.getName(), group.getName()) );
					group.setDisabled(outGroup.getDisabled());
					group.setDescription(outGroup.getDescription());
					group.setParentId( parentId );
					group.setSeqNo(groupDao.getNextSeqNo(parentId));
					group.setGroupType(Group.MAIN_GROUP_TYPE);
					groupDao.saveGroup(group);
					
					group.setDomain( domain );
					groupDao.refreshEntity(group);
					
					// 补齐权限
		            resourcePermission.addResource(group.getId(), group.getResourceType());
        		}
        		
    			group.setFromGroupId(fromGroupId);
            } 
            else {
            	group = (Group) temp.get(0);
            	temp = groupDao.getEntities("from Group t where t.parentId=?1 and t.name=?2", parentId, name);
        		if( EasyUtils.isNullOrEmpty(temp) ) {
        			group.setName(name);
        		} 
            }
            
            groupDao.refreshEntity(group);
            idMapping.put(fromGroupId, group.getId()); // 保存对应结果
            
            progress.add(1);  /* 更新进度信息 */
        }
    }
    
    private void syncUsers(List<?> users, Map<String, Long> idMapping, Progress progress) {
        List<String> loginNames = new ArrayList<String>();
        for (int i = 0; i < users.size(); i++) {
            UserDTO userDto = (UserDTO) users.get(i);
            userDto.setDisabled(userDto.getDisabled());
            
            // 如果用户登陆名相同，只保存第一个
            String userCode = userDto.getLoginName();
			if(loginNames.contains(userCode)) continue;
			
            // 如果用户所属的组不存在，则不导入该用户
            Long mainGroupId = idMapping.get(userDto.getGroupId());

            try { 
            	syncOneUser(userDto, mainGroupId);
            } 
            catch(Exception e) {
            	log.error("同步用户：" + userCode + "失败了: " + e.getMessage());
            }
            
            loginNames.add(userCode);
            progress.add(1);  /* 更新进度信息 */
        }
        
        // 如果循环结束了进度还没有完成，则取消进度（不取消会导致页面一直在请求进度信息）
        if( !progress.isCompleted() ) {
            progress.add(8888888); // 通过设置一个大数（远大于总数）来使进度完成
        }
    }

	protected void syncOneUser(UserDTO userDto, Long mainGroupId) {
		/* 检查相同账号的用户否已经存在: 
		 * 如果是之前同步过的，则只更新字段；
		 * 如果是已经存在的但不是从该fromApp同步过来的，则忽略该fromApp用户；
		 * 如果用户不存在，则新建。
		 */
		Group group = groupDao.getEntity(mainGroupId);
		
		// 检测用户是否非法：跨域同步等
		SyncDataHelper.checkSecurity(group, userDto);
		
		List<?> temp = groupDao.getEntities("from User t where ?1 in (t.loginName, t.email, t.telephone)", userDto.getLoginName());
		User user;
		if( temp.size() > 0 ) { // 更新已存在用户的信息
			user = (User) temp.get(0);
			updateUser(user, group, userDto);
		}
		else {
			user = new User();
		    SyncDataHelper.setUserByDTO(user, userDto);
			user.setGroupId(mainGroupId);
			
			Integer disabled = Math.max(user.getDisabled(), group.getDisabled()); // 如果组被停用了
			user.setDisabled(disabled);
			
			userDao.checkUserAccout(user);
			userDao.create(user);
			userDao.createObject(new GroupUser(user.getId(), mainGroupId));
		}
		
		userDao.recordUserLog(user, group, "sync");
	}

	public void updateUser(User user, Group nowGroup, UserDTO userDto) {
		Long userId = user.getId();
		Group oldGroup = groupDao.findMainGroupByUserId(userId);
		
		// 不能修改其它域的用户数据（除客户组）
		if( Group.CUSTOMER_GROUP.equals(oldGroup.getName())  || nowGroup.getDomain().equals(oldGroup.getDomain()) ) {
			user.setUserName(userDto.getUserName());
			Integer disabled = Math.max(userDto.getDisabled(), nowGroup.getDisabled()); // 如果组被停用了
			user.setDisabled(disabled);
			user.setFromUserId(userDto.getId());
			user.setEmail( (String) EasyUtils.checkNull(user.getEmail(), userDto.getEmail()) );
			user.setTelephone( (String) EasyUtils.checkNull(user.getTelephone(), userDto.getTelephone()) );
			
			userDao.checkUserAccout(user);
			userDao.refreshEntity(user);
			
			// 移动员工用户到指定组下（不能跨域移动）
			Long oldGroupId = oldGroup.getId();
			Long nowGroupId = nowGroup.getId();
			if( !nowGroupId.equals(oldGroupId) ) {
				String updateHQL = "update GroupUser set groupId=? where userId=?1 and groupId=?2";
				userDao.executeHQL(updateHQL, nowGroupId, userId, oldGroupId);
			}
		}
	}
}
