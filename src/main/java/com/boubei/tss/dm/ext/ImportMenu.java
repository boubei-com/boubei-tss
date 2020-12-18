/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ext;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.menu.INavigatorService;
import com.boubei.tss.modules.menu.Navigator;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;


public class ImportMenu implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	INavigatorService menuService = (INavigatorService) Global.getBean("NavigatorService");

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {
		
		File targetFile = new File(filepath);
		String json = FileHelper.readFile(targetFile);
            
        Long groupId = Long.parseLong(request.getParameter("groupId"));

        int count = createMenus(json, groupId);
        
		return "parent.alert('成功导入" +count+ "个菜单（按钮）.'); parent.loadInitData();";
	}

	/**
	 * 如果同名 且 同ID 的已存在，则覆盖； 通常是一个环境复制到另外一个环境
	 */
	public int createMenus(String json, Long groupId) throws Exception {
		
    	Map<Long, Long> idMapping = new HashMap<Long, Long>();
    	
    	List<?> list = EasyUtils.json2List(json);
    	int count = list.size();
    	
        for (int i = 0; i < count; i++) {
        	Object obj = list.get(i);  // Map
            Navigator menu = new ObjectMapper().readValue(EasyUtils.obj2Json(obj), Navigator.class);
            Long oldId = menu.getId();
            
            String hql = "from Navigator where name = ?1 and id = ?2";
            List<?> exists = Global.getCommonService().getList(hql, menu.getName(), oldId);
            if( exists.isEmpty() ) {
            	menu.setId(null);
            	
            	Long parentId = idMapping.get(menu.getParentId());
				menu.setParentId( (Long) EasyUtils.checkNull(parentId, groupId) );
				
            	menuService.saveNavigator(menu);
            }
            else {
            	Navigator old = (Navigator) exists.get(0);
            	BeanUtil.copy( old, menu, new String[]{"lockVersion"} );
            	menuService.saveNavigator(old);
            }
            
            idMapping.put(oldId, menu.getId());
        }
		return count;
	}
}
