/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.modules.param.ParamConstants;

public class MenuDTO {

	public Long id;
	public String name;   // 菜单（项）名称
	public String code;
	public String icon;
	
	public Long parentId; // 菜单项对应菜单
	public String url;    // url地址
	public String target; // 目标区域，_blank/_self 等

	public String methodName; // 方法名
	public String params;    // 参数
	
	public String description;

	public List<MenuDTO> children = new ArrayList<MenuDTO>();
	
	public MenuDTO(Navigator menu) {
		this.id = menu.getId();
		this.name = menu.getName();
		this.code = menu.getCode();
		this.parentId = menu.getParentId();
		this.url = menu.getUrl();
		this.target = menu.getTarget();
		this.params = menu.getParams();
		this.icon = menu.getIcon();
		this.description = menu.getDescription();
	}
	
	// 要求默认按 decode 排好序
	public static List<MenuDTO> buildTree(Long pId, List<Navigator> list) {
		List<MenuDTO> returnList = new ArrayList<MenuDTO>();
		Map<Long, MenuDTO> map = new HashMap<Long, MenuDTO>();
        
        for(Navigator menu : list) {
        	if(ParamConstants.TRUE.equals(menu.getDisabled())) {
        		continue; // 过滤掉停用的
        	}
        	
        	MenuDTO dto = new MenuDTO(menu);
        	map.put(menu.getId(), dto);
        	
        	if( pId.equals(menu.getParentId()) ) {
        		returnList.add(dto);
        	} 
        	else {
        		MenuDTO parent = map.get(menu.getParentId());
        		if( parent != null ) {
            		parent.children.add( dto );
        		}
        	}
        }
        
        return returnList;
	}
}