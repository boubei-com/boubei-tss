/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.web.display.grid.DefaultGridNode;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.framework.web.display.tree.ITreeTranslator;
import com.boubei.tss.framework.web.display.tree.LevelTreeParser;
import com.boubei.tss.framework.web.display.tree.StrictLevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.engine.FMSupportAction;
import com.boubei.tss.portal.engine.HTMLGenerator;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.entity.ReleaseConfig;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.Theme;
import com.boubei.tss.portal.service.IPortalService;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.XMLDocUtil;

@Controller
@RequestMapping("/auth/portal")
public class PortalAction extends FMSupportAction {
    
    @Autowired private IPortalService service;

    /**
     * 预览门户
     */
    @RequestMapping("/preview/{portalId}")
    public void previewPortal(HttpServletResponse response, HttpServletRequest request, 
    		@PathVariable("portalId") Long portalId)  {    
    	
    	Long themeId = EasyUtils.obj2Long(request.getParameter("themeId"));
    	Long pageId  = EasyUtils.obj2Long(request.getParameter("pageId"));
    	
        PortalNode portalNode = service.getPortal(portalId, themeId);
        HTMLGenerator gen = new HTMLGenerator(portalNode, pageId);

        printHTML(portalId, gen.toHTML(), true);
    }

    /**
     * 获取所有的Portal对象（取门户结构）并转换成Tree相应的xml数据格式
     */
    @RequestMapping("/list")
    public void getAllStructures4Tree(HttpServletResponse response) {
        List<?> data = service.getAllStructures();
        TreeEncoder encoder = new TreeEncoder(data, new StrictLevelTreeParser(PortalConstants.ROOT_ID));
        print("SourceTree", encoder);
    }
    
    // resourceId 有可能是："_root"， 此时传 0 过来。
    @RequestMapping("/operations/{resourceId}")
    public void getOperationsByResource(HttpServletResponse response, @PathVariable("resourceId") Long resourceId) {
        PermissionHelper permissionHelper = PermissionHelper.getInstance();
        String portalResourceType = PortalConstants.PORTAL_RESOURCE_TYPE;
        
        resourceId = (resourceId == 0) ? PortalConstants.ROOT_ID : resourceId;
        List<?> list = permissionHelper.getOperationsByResource(portalResourceType, resourceId);
        
        print("Operation", "p1,p2," + EasyUtils.list2Str(list) );
    }
           
    /**
     * <p>
     * 获取单个门户结构的节点信息，并转换成XForm相应的xml数据格式；
     * 如果该门户结构是根节点，则要一块取出其对应门户Portal的信息
     * </p>
     */
    @RequestMapping("/{id}")
    public void getStructureInfo(HttpServletResponse response, HttpServletRequest request, @PathVariable("id") Long id) {
        XFormEncoder encoder;
        if( DEFAULT_NEW_ID.equals(id) ) { // 如果是新增,则返回一个空的无数据的模板
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("type", request.getParameter("type"));
            
            Long parentId = Long.parseLong(request.getParameter("parentId"));
            map.put("parentId", parentId);
            
			if( !PortalConstants.ROOT_ID.equals(parentId) ) {
				Structure parent = service.getStructure(parentId);
				map.put("portalId", parent.getPortalId());
			}
            encoder = new XFormEncoder(PortalConstants.PORTALSTRUCTURE_XFORM, map);           
        }
        else {
            Structure info = service.getStructureWithTheme(id);            
            encoder = new XFormEncoder(PortalConstants.PORTALSTRUCTURE_XFORM, info);
            
            // 如果是门户节点，则带出主题信息列表
            if( info.isRootPortal() ) {
                List<?> data = service.getThemesByPortal(info.getPortalId());
                encoder.fixCombo("currentTheme.id", data, "id", "name", "|");
                encoder.setColumnAttribute("theme.name", "editable", "false");
            }           
        }
        print("DetailInfo", encoder);
    }
    
    /**
     * <p>
     * 保存门户结构信息，如果该门户结构是根节点，则要一块保存其门户Portal的信息。
     * </p>
     */    
    @RequestMapping(method = RequestMethod.POST)
    public void save(HttpServletResponse response, Structure ps) {
        boolean isNew = (ps.getId() == null);
        
        Structure structure;
        if(isNew) {        
        	String tempCode = ps.getCode();
            structure = service.createStructure(ps);
            
            /* 如果是在新增门户根节点时上传文件，则此时code和Id还没生成。code值 = 页面随机生成的一个全局变量。 */
            if( ps.getType().equals(Structure.TYPE_PORTAL) ) {
                File tempDir = Structure.getPortalResourceFileDir(tempCode);
                if(tempDir.exists()) {
                    tempDir.renameTo(structure.getPortalResourceFileDir());
                }
            } 
        } 
        else {
            structure = service.updateStructure(ps);
        }
            
        doAfterSave(isNew, structure, "SourceTree");
    }
    
    /**
     * 删除门户结构
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
        service.deleteStructure(id);
        printSuccessMessage();
    }
    
    /**
     * <p>
     * 停用/启用 门户结构（将其下的disabled属性设为"1"/"0"）
     * 停用时，如果有子节点，同时停用所有子节点（递归过程，子节点的子节点......)
     * 启用时，如果有父节点且父节点为停用状态，则启用父节点（也是递归过程，父节点的父节点......）
     * </p>
     */
    @RequestMapping("/disable/{id}/{state}")
    public void disable(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("state") int state) {
    	
        service.disable(id, state);
        printSuccessMessage();
    }
    
    /**
     * 排序，同层节点排序
     */
    @RequestMapping(value = "/sort/{id}/{targetId}/{direction}", method = RequestMethod.POST)
	public void sort(HttpServletResponse response, 
            @PathVariable("id") Long id, 
            @PathVariable("targetId") Long targetId,  
            @PathVariable("direction") int direction) {
    	
        service.sort(id, targetId, direction);
        printSuccessMessage();
    }
    
	/**
	 * 门户结构移动
	 */
	@RequestMapping(value = "/move/{id}/{container}", method = RequestMethod.POST)
	public void move(HttpServletResponse response, 
			@PathVariable("id") Long id, 
			@PathVariable("container") Long container) {
		
		service.move(id, container);
        printSuccessMessage("移动成功！");
	}
	
    //******************************** 以下为主题管理  ***************************************
    
    /**
     * 获取一个Portal的所有主题
     */
    @RequestMapping("/theme/list/{portalId}")
    public void getThemes4Tree(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> themeList = service.getThemesByPortal(portalId);
        
        Structure root = service.getStructure(portalId); 
        final Long defalutThemeId = root.getTheme().getId();
        
        TreeEncoder encoder = new TreeEncoder(themeList);
        encoder.setTranslator(new ITreeTranslator() {
            public Map<String, Object> translate(Map<String, Object> attributes) {
                if(defalutThemeId.equals(attributes.get("id"))) {
                    attributes.put("isDefault", "1");
                    attributes.put("icon", "images/default_theme.gif"); // 默认主题
                }
                return attributes;
            }
        });
        print("ThemeList", encoder);        
    }
    
    /**
     * 将Portal的一套主题另存为。。。
     */    
    @RequestMapping(value ="/theme/{themeId}/{name}", method = RequestMethod.POST)
    public void saveThemeAs(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId, @PathVariable("name") String name) {
    	
        Theme newTheme = service.saveThemeAs(themeId, name);       
        doAfterSave(true, newTheme, "ThemeList");
    }
    
    @RequestMapping(value ="/theme/rename/{themeId}/{name}", method = RequestMethod.PUT)
    public void renameTheme(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId, @PathVariable("name") String name) {
    	
        service.renameTheme(themeId, name);
        printSuccessMessage("重命名成功");
    }
    
    @RequestMapping(value ="/theme/default/{themeId}", method = RequestMethod.PUT)
    public void specifyDefaultTheme(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId) {
    	
         service.specifyDefaultTheme(themeId);
         printSuccessMessage();
    }
    
    @RequestMapping(value = "/theme/{themeId}", method = RequestMethod.DELETE)
    public void removeTheme(HttpServletResponse response, 
    		@PathVariable("themeId") Long themeId) {
    	
        service.removeTheme(themeId);
        printSuccessMessage();
    }
        
    //******************************** 以下为门户发布管理 ***************************************
    @RequestMapping("/release/list")
    public void getAllReleaseConfigs4Tree(HttpServletResponse response) {        
        List<?> list = service.getAllReleaseConfigs();
		TreeEncoder encoder = new TreeEncoder(list); 
        print("ReleaseConfigTree", encoder);
    }
    
    @RequestMapping("/release/{id}")
    public void getReleaseConfig(HttpServletResponse response, @PathVariable("id") Long id) {
        XFormEncoder encoder = null;
        if( DEFAULT_NEW_ID.equals(id) ) {
            encoder = new XFormEncoder(PortalConstants.RELEASE_XFORM_TEMPLET);           
        }
        else{
            ReleaseConfig rconfig = service.getReleaseConfig(id);            
            encoder = new XFormEncoder(PortalConstants.RELEASE_XFORM_TEMPLET, rconfig);
            
            Long portalId = rconfig.getPortal().getId();
            List<?> data = service.getThemesByPortal(portalId);
            encoder.fixCombo("theme.id", data, "id", "name", "|");
        }        
        print("ReleaseConfig", encoder);
    }
 
    @RequestMapping(value ="/release", method = RequestMethod.POST)
    public void saveReleaseConfig(HttpServletResponse response, ReleaseConfig rconfig) {
        boolean isNew = rconfig.getId() == null ? true : false;             
        rconfig = service.saveReleaseConfig(rconfig);         
        doAfterSave(isNew, rconfig, "ReleaseConfigTree");
    }
    
    @RequestMapping(value = "/release/{id}", method = RequestMethod.DELETE)
    public void removeReleaseConfig(HttpServletResponse response, @PathVariable("id") Long id) {
        service.removeReleaseConfig(id);
        printSuccessMessage();
    }
    
    @RequestMapping("/activePortals")
    public void getActivePortals4Tree(HttpServletResponse response) {
        List<?> data = service.getActivePortals();
        TreeEncoder encoder = new TreeEncoder(data, new StrictLevelTreeParser(PortalConstants.ROOT_ID));
        encoder.setNeedRootNode(false);
        print("SourceTree", encoder);
    }
    
    @RequestMapping("/theme4release/{portalId}")
    public void getThemesByPortal(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> data = service.getThemesByPortal(portalId);
        Object[] objs = EasyUtils.list2Combo(data, "id", "name", "|");
        String returnStr = "<column name=\"theme.id\" caption=\"主题\" mode=\"string\" " +
        		" values=\"" + objs[0] + "\" texts=\"" + objs[1] + "\"/>";
        
        print("ThemeList", returnStr);
    }
    
    @RequestMapping("/activePages/{portalId}")
    public void getActivePagesByPortal4Tree(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> data = service.getActivePagesByPortal(portalId);
        TreeEncoder encoder = new TreeEncoder(data, new LevelTreeParser());
        encoder.setNeedRootNode(false);
        print("PageTree", encoder);
    }
       
    //******************************* 以下为门户缓存管理 ***************************************
    @RequestMapping("/cache/{portalId}")
    public void cacheManage(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> data = service.getThemesByPortal(portalId);
        StringBuffer sb= new StringBuffer("<actionSet>");
        for( Object temp : data ) {
            Theme theme = (Theme) temp;
            sb.append("<cacheItem id=\"").append(theme.getId()).append("\" name=\"").append(theme.getName()).append("\" />");
        }
        print("CacheManage", sb.append("</actionSet>").toString());
    }
    
    @RequestMapping(value = "/cache/{portalId}/{themeId}", method = RequestMethod.DELETE)
    public void flushCache(HttpServletResponse response, 
    		@PathVariable("portalId") Long portalId, 
    		@PathVariable("themeId") Long themeId) {
    	
        Pool pool = CacheHelper.getLongCache();        
        pool.destroyByKey(PortalConstants.PORTAL_CACHE + "-" + portalId + "-" + themeId);
        printSuccessMessage();
    }
    
    //******************************* 获取门户流量信息 ******************************************
    
    @RequestMapping("/flowrate/{portalId}")
    public void getFlowRate(HttpServletResponse response, @PathVariable("portalId") Long portalId) {
        List<?> rateItems = service.getFlowRate(portalId);
        List<IGridNode> gridList = new ArrayList<IGridNode>();
        for(Iterator<?> it = rateItems.iterator(); it.hasNext();) {
            Object[] objs = (Object[]) it.next();
            DefaultGridNode gridNode = new DefaultGridNode();
            gridNode.getAttrs().put("name", objs[0]);
            gridNode.getAttrs().put("rate", objs[1]);
            gridList.add(gridNode);
        }
        
        StringBuffer template = new StringBuffer();
        template.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><grid version=\"2\"><declare sequence=\"true\">");
        template.append("<column name=\"name\" caption=\"页面名称\" mode=\"string\" align=\"center\"/>");
        template.append("<column name=\"rate\" caption=\"访问次数\" mode=\"string\" align=\"center\"/>");
        template.append("</declare><data></data></grid>");
        
        GridDataEncoder gEncoder = new GridDataEncoder(gridList, XMLDocUtil.dataXml2Doc(template.toString()));
        print("PageFlowRate", gEncoder);
    }
}