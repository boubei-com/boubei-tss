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
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.tree.ITreeTranslator;
import com.boubei.tss.framework.web.display.tree.LevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.tree.TreeNode;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.portal.engine.FMSupportAction;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.helper.ComponentHelper;
import com.boubei.tss.portal.service.IComponentService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.URLUtil;
import com.boubei.tss.util.XMLDocUtil;
import com.boubei.tss.util.XmlUtil;

import freemarker.template.TemplateException;

@Controller
@RequestMapping("/auth/component")
public class ComponentAction extends FMSupportAction {
 
    @Autowired private IComponentService service;

    @RequestMapping("/list")
    public void getAllComponents4Tree(HttpServletResponse response) {
        List<?> data = service.getAllComponentsAndGroups();
        TreeEncoder encoder = new TreeEncoder(data, new LevelTreeParser());        
        encoder.setNeedRootNode(false);
        print("SourceTree", encoder);
    }

    /**
     * 所有启动的组件树型展示.（编辑门户结构的时候用到）
     */
    @RequestMapping("/enabledlist/{type}")
    public void getEnabledComponents4Tree(HttpServletResponse response, @PathVariable("type") int type) {
        List<?> data = service.getEnabledComponentsAndGroups(type);
        
        TreeEncoder encoder = new TreeEncoder(data, new LevelTreeParser());        
        encoder.setNeedRootNode(false);
        
        encoder.setTranslator( new ITreeTranslator() {
            public Map<String, Object> translate(Map<String, Object> attributes) {
                if( Boolean.TRUE.equals(attributes.get("isGroup")) ) { // 使得组节点不可选
                    attributes.put(TreeNode.TREENODE_ATTR_CANSELECTED, "0"); 
                }
                return attributes;
            }           
        });
        print("SourceTree", encoder);
    }

    /**
     * 获取组件详细信息.
     */
    @RequestMapping("/{groupId}/{id}")
    public void getComponentInfo(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("groupId") Long groupId) {
    	
        XFormEncoder encoder;
        if ( DEFAULT_NEW_ID.equals(id) ) {   // 新增组件
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("parentId", groupId);
            
            Component componentGroup = service.getComponent(groupId);
            String templatePath = componentGroup.getTemplatePath();
            encoder = new XFormEncoder(templatePath, map);
        } 
        else { // 修改组件
            Component component = service.getComponent(id);
            if(component == null) {
            	throw new BusinessException(EX.P_01 + id);
            }
            Component componentGroup = service.getComponent(component.getParentId());
            String templatePath = componentGroup.getTemplatePath();
			encoder = new XFormEncoder(templatePath, component);
        }
        print("DetailInfo", encoder);
    }

    /**
     * 新增元素（组）.
     */
    @RequestMapping(method = RequestMethod.POST)
    public void save(HttpServletResponse response, Component component) {
        boolean isNew = component.getId() == null ? true : false;      
        component = service.saveComponent(component);
        doAfterSave(isNew, component, "SourceTree");
    }
    
    @RequestMapping(value = "/rename/{id}/{name}")
    public void renameGroup(HttpServletResponse response, 
    		@PathVariable("id") Long id, @PathVariable("name") String name) {
    	Component component = service.getComponent(id);
    	component.setName(name);
    	service.saveComponent(component);
    	
        printSuccessMessage("修改成功");
    }

    /**
     * 删除组件(组)
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
        service.deleteComponent(id);
        printSuccessMessage("删除成功");
    }

    /**
     * 停用/启用 组件（将其disabled属性设为"1"/"0"）
     */
    @RequestMapping("/disable/{id}/{state}")
    public void disable(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("state") int state) {
    	
        service.disableComponent(id, state);
        printSuccessMessage();
    }
    
    /**
     * 组件排序
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
     * 移动
     */
	@RequestMapping(value = "/move/{id}/{groupId}", method = RequestMethod.POST)
    public void moveTo(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("groupId") Long groupId) {
		
        service.moveComponent(id, groupId);
        printSuccessMessage();
    }
 
    /**
     * 导出组件
     */
    @RequestMapping(value = "/export/{id}", method = RequestMethod.GET)
    public void exportComponent(HttpServletResponse response, @PathVariable("id") Long id) {   
        Component component = service.getComponent(id);
        String desDir = URLUtil.getWebFileUrl(component.getResourceBaseDir()).getPath(); 
        
        ComponentHelper.exportComponent(desDir, component, component.getComponentType() + ".xml");
    }
    
    /**********************************************  在线编辑组件参数配置    *******************************************/
    
    /**
     * 获取组件参数,并拼装成一个xml返回
     */
    @RequestMapping("/params/{id}")
    public void getDefaultParams4Xml(HttpServletResponse response, @PathVariable("id") Long id) {
        Component component = service.getComponent(id);
        String componentType = component.getComponentType();
        
        StringBuffer sb = new StringBuffer("<" + componentType + " ");
        String xpath = "//" + componentType + "/parameters/param";
        List<?> parameters = XMLDocUtil.dataXml2Doc(component.getDefinition()).selectNodes(xpath);
        for (int i = 0; i < parameters.size(); i++) {
            Element param = (Element) parameters.get(i);
            String name = param.attributeValue("name");
            String defaultValue = param.attributeValue("defaultValue");
            sb.append(name).append("=\"").append(XmlUtil.toFormXml(defaultValue)).append("\" ");
        }
        sb.append(">").append("model/" + componentType + "/" + component.getCode() + "/" + Component.PARAM_FILE);                    

        print("ComponentParams", sb.append("</" + componentType + ">").toString());
    }
    
    /**
     * 在线编辑组件参数配置  
     */
	@RequestMapping("/paramconfig/{id}")
    public void getComponentParamsConfig(HttpServletResponse response, HttpServletRequest request, 
    		@PathVariable("id") Long id) {
		
        Component component = service.getComponent(id);
       
        String configFilePath = URLUtil.getWebFileUrl(component.getResourcePath() + "/" + Component.PARAM_FILE).getFile();
        if( !new File(configFilePath).exists() ) {
            // 如果是第一次配置，且配置参数项不为空，则根据需要的参数项自动生成一个默认模板
            Document doc = DocumentHelper.createDocument();
            Element xformNode = doc.addElement("Response").addElement("ConfigParams").addElement("xform");
            Element declareNode = xformNode.addElement("declare");
            Element layoutNode = xformNode.addElement("layout");
            xformNode.addElement("data");
            
            //  paramsItem 格式类似 ：" bgColor=red \n menuId=12 "
            String paramsItem = EasyUtils.obj2String( request.getParameter("paramsItem") );
            StringTokenizer stk = new StringTokenizer(paramsItem);
            while(stk.hasMoreTokens()) {
                String itemName = stk.nextToken(); 
                int index = itemName.indexOf("=");
                itemName = itemName.substring(0, index);
                
                //<column name="tableWidth" caption="XX" mode="string"/>
                Element itemcolumn = declareNode.addElement("column");
                itemcolumn.addAttribute("name", itemName);
                itemcolumn.addAttribute("caption", itemName);
                itemcolumn.addAttribute("mode", "string");
                
                /*<TR>
                 *   <TD width="50"><label binding="tableWidth"/></TD>
                 *   <TD><input binding="tableWidth" type="text"/></TD>
                 *</TR>
                 */
                Element trNode = layoutNode.addElement("TR");
                Element tdNode1 = trNode.addElement("TD");
                tdNode1.addAttribute("width", "50");
                Element labelNode = tdNode1.addElement("label");
                labelNode.addAttribute("binding", itemName);
                
                Element tdNode2 = trNode.addElement("TD");
                Element inputNode = tdNode2.addElement("input");
                inputNode.addAttribute("binding", itemName);
                inputNode.addAttribute("type", "text");
			}
			File dir = new File(configFilePath).getParentFile();
			FileHelper.createDir(dir.getPath());

			FileHelper.writeXMLDoc(doc, configFilePath);
			print(XMLDocUtil.createDocByAbsolutePath(configFilePath).asXML());
        } 
        else {
            print(XMLDocUtil.createDocByAbsolutePath(configFilePath).asXML());
        }
    }
    
	@RequestMapping(value = "/paramconfig/{id}", method = RequestMethod.POST)
    public void saveComponentParamsConfig(HttpServletResponse response, HttpServletRequest request, 
    		@PathVariable("id") Long id) {
		
        Component component = service.getComponent(id);
        String configFile = URLUtil.getWebFileUrl(component.getResourcePath() + "/paramsXForm.xml").getFile();
        File cfFileDir = new File(configFile).getParentFile();
        FileHelper.createDir( cfFileDir.getPath() );
        
        String configXML = request.getParameter("configXML");
        Document doc = XMLDocUtil.dataXml2Doc("<Response>\n<ConfigParams>\n" + configXML + "\n</ConfigParams>\n</Response>");
        FileHelper.writeXMLDoc(doc, configFile);
        
        printSuccessMessage("保存成功");
    }
    
    /********************************************************************************************************************/
    /***************************************************      预览组件    ************************************************/
    /********************************************************************************************************************/

    /**
     * 获取组件的XML结构数据，用于单个组件预览。
     * @return
     * @throws IOException
     * @throws TemplateException
     */
	@RequestMapping("/preview/{id}")
    public void  previewComponent(HttpServletResponse response, @PathVariable("id") Long id) throws IOException, TemplateException {
        Component component = service.getComponent(id);
        String componentType = component.getComponentType();
        
        Document doc = XMLDocUtil.dataXml2Doc(component.getDefinition());
 
        String html   = doc.selectSingleNode("/" + componentType + "/html").getText();
        String script = doc.selectSingleNode("/" + componentType + "/script").getText();
        String style  = doc.selectSingleNode("/" + componentType + "/style").getText();
        
        Map<String, String> events = new HashMap<String, String>();
        List<?> eventNodes = doc.selectNodes("/" + componentType + "/events/attach");
        for(Iterator<?> it = eventNodes.iterator(); it.hasNext();) {
            Element eventNode = (Element) it.next();
            events.put(eventNode.attributeValue("event"), eventNode.attributeValue("onevent"));
        }   
        
        Map<String, String> parameters = new HashMap<String, String>();
        List<?> paramNodes = doc.selectNodes("/" + componentType + "/parameters/param");
        for(Iterator<?> it = paramNodes.iterator(); it.hasNext();) {
            Element paramNode = (Element) it.next();
            parameters.put("#{" + paramNode.attributeValue("name") + "}", paramNode.attributeValue("defaultValue"));
        } 
        
        parameters.put("${id}", componentType.substring(0, 1) + component.getId());
        parameters.put("${content}", "");
        parameters.put("${basepath}", Environment.getContextPath() + "/" + component.getResourcePath() + "/");

        // 直接预览门户组件
        for(int i = 0; i < 12; i++) {
        	parameters.put("${port" + i + "}", "port" + i);
        }
        String data = toHTML(html, script, style, events, parameters);
        printHTML(data);
    }
    
    private String toHTML(String html, String script, String style, 
            Map<String, String> events, Map<String, String> parameters) {
        
        StringBuffer sb = new StringBuffer("<html>\n<head>\n");               
        sb.append("<style>\n").append(MacrocodeCompiler.run(style, parameters, true)).append("\n</style>\n");        
        
        String commonJSPath = Environment.getContextPath() + "/tools/";
        sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.js\"></script>\n");
        sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.ajax.js\"></script>\n");
        
        sb.append("<script language=\"javascript\">\n");
        sb.append(MacrocodeCompiler.run(script, parameters, true));
        sb.append("\n").append(getEvents4HTML(events, parameters)).append("\n</script>\n");
        sb.append("</head>\n<body>\n");
        sb.append(MacrocodeCompiler.run(html, parameters, true));
        sb.append("\n</body></html>");
        
        return sb.toString();
    }
    
    private StringBuffer getEvents4HTML(Map<String, String> events, Map<String, String> parameters) {
        StringBuffer sb = new StringBuffer();
        StringBuffer onloadEvent = new StringBuffer();
        for( Entry<String, String> entry : events.entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            value = MacrocodeCompiler.run(value, parameters, true);
            
            if ("onload".equals(key)) {
                onloadEvent.append("window.onload = function() {\n").append(value + "();\n};\n");
            } 
            else {
                sb.append("window." + key + "=" + value + "\n"); 
            }
        }
        return sb.append(onloadEvent);
    }
}