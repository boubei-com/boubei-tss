/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.engine.macrocode.AbstractMacrocodeContainer;
import com.boubei.tss.portal.engine.macrocode.MacrocodeContainerFactory;
import com.boubei.tss.portal.engine.model.AbstractElementNode;
import com.boubei.tss.portal.engine.model.DecoratorConfigable;
import com.boubei.tss.portal.engine.model.DecoratorNode;
import com.boubei.tss.portal.engine.model.IPageElement;
import com.boubei.tss.portal.engine.model.LayoutNode;
import com.boubei.tss.portal.engine.model.Node;
import com.boubei.tss.portal.engine.model.PageNode;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.engine.model.PortletNode;
import com.boubei.tss.portal.entity.FlowRate;
import com.boubei.tss.portal.helper.FlowrateManager;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;

/**
 * <p> HTML页面代码生成器 </p>
 * <p>
 * 可以根据Node节点生成相应的HTML页面文件信息
 * </p>
 */
public class HTMLGenerator {
    
    Logger log = Logger.getLogger(HTMLGenerator.class);
    
    /**
     * 页面关键字
     */
    private List<String> keyword = new NoNullLinkedList<String>();

    /**
     * 页面外挂js文件路径列表
     */
    private List<String> scriptFiles = new NoNullLinkedList<String>();

    /**
     * 页面外挂css文件路径列表
     */
    private List<String> styleFiles = new NoNullLinkedList<String>();

    /**
     * 页面脚本列表：页面下所有显示元素使用到的script脚本
     */
    private List<String> scriptCodes = new NoNullLinkedList<String>();

    /**
     * 页面样式表列表：页面下所有显示元素使用到的样式表脚本
     */
    private List<String> styleCodes = new NoNullLinkedList<String>();
 
    /**
     * 事件脚本
     */
    private List<String[]> eventCodes = new NoNullLinkedList<String[]>();

    /**
     * 门户结构自定义的初始化脚本： 这些信息输出到html里只为了万一页面出错时检查用
     */
    private List<String> initCodes = new LinkedList<String>();

    /**
     * 预览元素的路径（门户结构树上路径）： Portlet实例，版面，……，版面，页面
     */
    private List<Node> treePath = new ArrayList<Node>();

    /**
     * 门户加载的外部JS、CSS文件目录访问地址
     */
    private String portalResourseDir;
    
    /**
     * 页面标题
     */
    private String title; 

    /**
     * 页面对应的自定义DOM对象
     */
    private Element dom = new Element();
    
    /**
     * HTML页面生成器构造函数：用于生成完整页面HTML代码<br>
     * <br>
     * @param portal
     *            门户结构PortalNode节点 
     * @param id
     *            所要显示门户结构中某节点ID，可能是page、section or portletInstance
     */
    public HTMLGenerator(PortalNode portal, Long id) {
        this.title = portal.getName();
        this.portalResourseDir = getPortalResourceDir(portal);
        
        PageNode page;
        Map<Long, Node> nodesMap = portal.getNodesMap();
        Set<Node> children = portal.getChildren();
        if( children.isEmpty() ) {
        	throw new BusinessException("no page is visible");
        }
        
		if (id == null || id == 0 || !nodesMap.containsKey(id) ) { // 默认浏览第一个页面
            page = (PageNode) children.toArray()[0]; 
        } 
        else { // 浏览指定的门户节点所在的页面
            Node content = nodesMap.get(id); 
            
            // 调整指定预览的节点至所在分支的第一个节点，以保证优先被生成
            Set<Node> brothers = content.getParent().getChildren();
			brothers.remove(content);
			
			Set<Node> set = new LinkedHashSet<Node>(); 
			set.add(content);
			set.addAll(brothers);
			
			brothers.clear();
			brothers.addAll(set);
            
            // 往上查找需要显示节点的父亲节点，直到门户根节点，把整个路径加入到treePath中来（不包括根节点）
            while ( !portal.equals(content) ) {
                treePath.add(content);
                content = content.getParent();
            }
            page = (PageNode) treePath.get(treePath.size() - 1); // path最后为PageNode
        }
        
        title = portal.getName() + "-" + page.getName();
        keyword.add(portal.getName());
        scriptFiles.addAll(portal.getScriptFiles());
        styleFiles.addAll(portal.getStyleFiles());
        scriptCodes.add(portal.getScriptCode());
        styleCodes.add(portal.getStyleCode());
        
        dom = new Element(page);
        
        // 此处加入页面流量统计
        FlowrateManager.getInstanse().output(new FlowRate(page.getId(), Environment.getClientIp()));
    }
    
    /** 获取门户对应外部链接JS、CSS文件访问路径 */
    private String getPortalResourceDir(PortalNode node) {
        String dirName = node.getCode() + "_" + node.getPortalId();
        return Environment.getContextPath() + PortalConstants.PORTAL_MODEL_DIR + "/" + dirName + "/";
    }

    /**
     * <p>
     * 生成页面HTML代码
     * </p>
     * @return String 当前页面的HTML代码
     */
    public String toHTML() {
        StringBuffer sb = new StringBuffer();
        sb.append("<!DOCTYPE HTML>\n");
        sb.append("<HTML xmlns:TSS xmlns:WorkSpace xmlns:Tree xmlns:Grid xmlns:Form>\n");
        sb.append("<HEAD>\n");
        sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<TITLE>").append(title).append("</TITLE>\n");
        sb.append(formatKeywords());
        sb.append(formatStyleLinks());
        sb.append(formatScriptLinks());
        sb.append("<style>\n").append(formatStyleCodes()).append("</style>\n");
        sb.append("<script language=\"javascript\">\n").append(formatScriptCodes()).append("</script>\n");
        sb.append("</HEAD>\n");
        return sb.append(dom).append("</HTML>").toString();
    }
 
    /**
     * 返回门户页面的关键字字符串
     */
    private String formatKeywords() {
        return "<META NAME=\"Keywords\" CONTENT=\"" + EasyUtils.list2Str(keyword) + "\">\n";
    }

    /**
     * 导入返回外挂样式表文件标签
     */
    private StringBuffer formatStyleLinks() {
        StringBuffer sb = new StringBuffer();
        if( styleFiles.size() > 0 ) {
        	for ( String filePath : styleFiles ) {
                sb.append("<link href=\"" + (portalResourseDir + filePath) + "\" rel=\"stylesheet\">\n");
            }
        }
        else {
        	// 默认挂载的css
            String commonCSSPath = Environment.getContextPath() + "/tools/tssJS/css/";
            sb.append("<link href=\"" + commonCSSPath + "boubei.css\" rel=\"stylesheet\">\n");
        }
        
        return sb;
    }

    /**
     * 导入返回外挂脚本文件标签
     */
    private StringBuffer formatScriptLinks() {
        StringBuffer sb = new StringBuffer();
        String commonJSPath = Environment.getContextPath() + "/tools/";
        
        if( scriptFiles.size() > 0 ) {
            sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.js\"></script>\n");
            sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.ajax.js\"></script>\n");
        	for ( String filePath : scriptFiles ) {
                sb.append("<script src=\"" + (portalResourseDir + filePath) + "\"></script>\n");
            }
        }
        else {
        	// 默认挂载的js
            sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.all.js\"></script>\n");
            sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.json2Form.js\"></script>\n");
            sb.append("<script src=\"" + commonJSPath + "tssJS/tssJS.jsonp.js\"></script>\n");
            sb.append("<script src=\"" + commonJSPath + "portlet.js\"></script>\n");
        }
        
        return sb;
    }
 
    /**
     * 所有JS脚本代码
     */
    private StringBuffer formatScriptCodes() {
        // 简单脚本代码：不包含事件部分代码
        StringBuffer sb = new StringBuffer();
        for( String script : scriptCodes ) {
            sb.append(script + "\n");
        }
        sb.append("\n");
        
        StringBuffer onloadEvent = new StringBuffer("window.onload = function() { \n");
        for( String initCode : initCodes ) {
            onloadEvent.append("  " + initCode).append("\n"); // 将每个node生成的js code换行分隔开来
        }
        sb.append("\n");
        
        for( String[] codes : eventCodes ) {
        	onloadEvent.append( EasyUtils.checkTrue("onload".equals(codes[0]), "  " +codes[1]+ "();\n", "") ).append("\n");
        }
        onloadEvent.append("};").append("\n"); 
        
        return sb.append(onloadEvent);
    }
 
    /**
     * <p>
     * 所有样式表代码
     * </p>
     * @return StringBuffer
     */
    public StringBuffer formatStyleCodes() {
        StringBuffer sb = new StringBuffer();               
        for ( String style : styleCodes ) {
            sb.append(style).append("\n");
        }
        return sb;
    }

    /**
     * <p>
     * 页面生成时使用的自定义Element对象，用于生成页面HTML代码
     * </p>
     */
    public class Element {

        /**
         * 节点HTML片段对象
         */
        private List<String> htmlFragments = new ArrayList<String>();

        /**
         * 空对象，用于空门户的展示
         */
        public Element() {
            this.htmlFragments.add("<body></body>");
        }

        /**
         * 生成局部页面为XML时，为生成某个版面或Portlet实例HTML而实例化DOM对象。
         *
         * @param node
         *             版面或Portlet实例
         * @param isPreviewPage
         *             true:  预览页面， 根据页面、版面下的子节点（IPageElement）创建Element对象 
         *             false: 生成(局部页面:IPageElement)XML
         */
        public Element(IPageElement node) { 
        	Long id = node.getId();
            String elementType = node.getPageElementType();
           
            HTMLGenerator.this.keyword.add(node.getName());
            
            // 创建页面元素（包括版面和portletInstance）上下文关系的初始化代码， 定义门户结构关系
            HTMLGenerator.this.initCodes.add(appendElementType(id, elementType)); 
            HTMLGenerator.this.initCodes.add(appendElementParent(id, node.getParent().getId())); 
 
            createIPageElement(node);
        }

        /**
         * 创建页面元素（包括版面和portletInstance）的HTML代码
         */
        private void createIPageElement(IPageElement node) {
            String pageElementType = node.getPageElementType();
            this.htmlFragments.add("\n<!-- (" + pageElementType + ")" + ((Node)node).getName() + " start-->\n");
            this.htmlFragments.add("<div class=\"" + pageElementType + "\" id=\"" + ((Node)node).getId() + "\">");
            
            DecoratorNode decoratorNode = ( (DecoratorConfigable) node).getDecoratorNode();
			this.htmlFragments.add(new Element(decoratorNode).toHTML());
            this.htmlFragments.add("</div>\n");
            this.htmlFragments.add("\n<!-- (" + pageElementType + ")" + ((Node)node).getName() + " end-->\n");
        }
 
        // 以下这些信息输出到html里为了万一页面出错时检查用
        private String appendElementType( Object id, String type ) { // 定义门户结构的类型
        	return "$$('" + id + "').type = '" + type + "';\n"; 
        }
        private String appendElementParent( Object id, Long parentId ) { // 定义门户结构父子关系
        	return "$$('" + id + "').parent = $$('" + parentId + "');\n";  
        }
        private String appendElementIndex( Object id, int index ) {
        	return "$$('" + id + "').index = " + index + ";\n";
        }
        private String appendElementCIndex( Object id, int cIndex ) {
        	return "$$('" + id + "').cIndex = " + cIndex + ";\n";
        }
 
        /**
         * Element对象构造函数，根据PageNode创建Element对象
         */
        public Element(PageNode node) {
            HTMLGenerator.this.keyword.add(node.getName());
            HTMLGenerator.this.scriptFiles.addAll(node.getScriptFiles());
            HTMLGenerator.this.styleFiles.addAll(node.getStyleFiles());
            HTMLGenerator.this.scriptCodes.add(node.getScriptCode());
            HTMLGenerator.this.styleCodes.add(node.getStyleCode());
            
            HTMLGenerator.this.initCodes.add(appendElementType(node.getId(), "Page"));  
            HTMLGenerator.this.initCodes.add("$$('" + node.getId() + "').portalId = " + node.getPortalId() + ";\n");
            
            this.htmlFragments.add("<body id=\"" + node.getId() + "\">" + new Element(node.getDecoratorNode()) + "</body>");
        }

        /**
         * Element对象构造函数，根据DecoratorNode创建Element对象
         */
        public Element(DecoratorNode node) {
        	Long id = node.getId();
        	Long parentId = node.getParent().getId();
        	
            HTMLGenerator.this.scriptCodes.add(MacrocodeContainerFactory.newInstance(node.getScript(), node).compile());
            HTMLGenerator.this.styleCodes.add (MacrocodeContainerFactory.newInstance(node.getStyle(),  node).compile());
            
            // 创建页面修饰器上下文关系的初始化代码
            HTMLGenerator.this.initCodes.add(appendElementType("D" + parentId, "Decorator"));  
            HTMLGenerator.this.initCodes.add(appendElementParent("D" + parentId, parentId)); 
            HTMLGenerator.this.initCodes.add("$$('"  + parentId + "').decorator = $$('D" + parentId + "');\n");
            HTMLGenerator.this.initCodes.add("$$('D" + parentId + "').decoratorId = " + id + ";\n");
            
            // 添加事件
            appendEventCodes(node); 
            
            this.htmlFragments.add("\n<!-- (修饰器)" + node.getName() + " start-->\n");
            DecoratorConfigable parent = (DecoratorConfigable) node.getParent();
            Element content = createDecorateContent(parent.getDecoratorContent()); // 被修饰的内容
            this.htmlFragments.add(MacrocodeContainerFactory.newInstance(node.getHtml(), node, content).compile());
            this.htmlFragments.add("\n<!-- (修饰器)" + node.getName() + " end-->\n");
        }
        
        /**
         * 创建修饰器修饰的元素（或叫修饰器的内容： layout或portlet)的Element对象
         */
        private Element createDecorateContent(AbstractElementNode node){
            return (node instanceof LayoutNode) ? new Element((LayoutNode)node) : new Element((PortletNode)node);
        }
 
        /**
         * 设置布局器/修饰器/portlet相关事件脚本。
         * 注意：各个门户结构的事件执行顺序有先后，按门户结构从上往下的顺序执行。
         */
        private void appendEventCodes(AbstractElementNode node) {
            for( Entry<String, String> entry : node.getEvents().entrySet() ){
                AbstractMacrocodeContainer macrocodeContainer = MacrocodeContainerFactory.newInstance(entry.getValue(), node);
				HTMLGenerator.this.eventCodes.add(new String[] { entry.getKey(), macrocodeContainer.compile() } );
            }
        }

        /**
         * Element对象构造函数，根据PortletNode创建Element对象
         */
        public Element(PortletNode node) {
        	Long id = node.getId();
        	Long parentId = node.getParent().getId();
        	
            HTMLGenerator.this.keyword.add(node.getName());
            HTMLGenerator.this.scriptCodes.add(MacrocodeContainerFactory.newInstance(node.getScript(), node).compile());
            HTMLGenerator.this.styleCodes.add (MacrocodeContainerFactory.newInstance(node.getStyle(),  node).compile());
            
            // 创建页面Portlet上下文关系的初始化代码
            HTMLGenerator.this.initCodes.add(appendElementType("P" + parentId, "Portlet"));  
            HTMLGenerator.this.initCodes.add(appendElementParent("P" + parentId, parentId)); 
            HTMLGenerator.this.initCodes.add("$$('"  + parentId + "').portlet = $$('P" + parentId + "');\n");
            HTMLGenerator.this.initCodes.add("$$('P" + parentId + "').portletId = " + id + ";\n");
            
            // 添加事件
            appendEventCodes(node); 
            
            this.htmlFragments.add("\n<!-- (Portlet)" + node.getName() + " start-->\n");
            this.htmlFragments.add(MacrocodeContainerFactory.newInstance(node.getHtml(), node).compile());
            this.htmlFragments.add("\n<!-- (Portlet)" + node.getName() + " end-->\n");
        }

        /**
         * Element对象构造函数，根据LayoutNode创建Element对象
         */
        public Element(LayoutNode node) {
            Node parent = node.getParent();
        	Long id = node.getId();
        	Long parentId = parent.getId();

            HTMLGenerator.this.scriptCodes.add(MacrocodeContainerFactory.newInstance(node.getScript(), node).compile());
            HTMLGenerator.this.styleCodes.add (MacrocodeContainerFactory.newInstance(node.getStyle(),  node).compile());
            
            // 创建页面布局器上下文关系的初始化代码
            HTMLGenerator.this.initCodes.add(appendElementType("L" + parentId, "Layout"));  
            HTMLGenerator.this.initCodes.add(appendElementParent("L" + parentId, parentId)); 
            HTMLGenerator.this.initCodes.add("$$('"  + parentId + "').layout = $$('L" + parentId + "');\n");
            HTMLGenerator.this.initCodes.add("$$('L" + parentId + "').layoutId = " + id + ";\n");
            
            appendEventCodes(node); // 事件           
                       
            // 创建所有子节点对应的Element对象集合
            int portNumber = node.getPortNumber();
            List<List<String>> childIds = new ArrayList<List<String>>();  // 二维List，数据如  ： [[child1, child2], [child3], [child4]]
            Map<String, Element> portMappingElement = createChildElements(parent, portNumber, childIds);
           
            // 补上子节点所在版面位置索引号及子索引号定义脚本
            for (int i = 0; i < childIds.size(); i++) {
                List<?> items = childIds.get(i);
                for (int j = 0; j < items.size(); j++) {
                    HTMLGenerator.this.initCodes.add(appendElementIndex(items.get(j), i));
                    HTMLGenerator.this.initCodes.add(appendElementCIndex(items.get(j), j));
                }
            }
            
            // 补上布局器所在版面（或页面）与子节点的父子关系定义脚本
            HTMLGenerator.this.initCodes.add(getChildrenRelations(childIds, parent.getId())); 
            
            this.htmlFragments.add("\n<!-- (布局器)" + node.getName() + " start-->\n");
            this.htmlFragments.add(MacrocodeContainerFactory.newInstance(node.getHtml(), node, portMappingElement).compile());
            this.htmlFragments.add("\n<!-- (布局器)" + node.getName() + " end-->\n");
        }

        /**
         * <p>
         * 获取布局器所在版面（或页面）与子节点的父子关系定义脚本
         * <pre>
         * $$('P1').subset = [
         *        [$$('child1'), $$('child2')],
         *        [$$('child3')],
         *        [$$('child4')]
         *     ]
         * </pre>
         * </p>
         * @param childIds
         * @param parentId
         * @return StringBuffer
         */
        private String getChildrenRelations(List<List<String>> childIds, Long parentId) {
            StringBuffer sb = new StringBuffer();
            sb.append("$$('" + parentId + "').subset = [");
            for (int i = 0; i < childIds.size(); i++) {
                sb.append( EasyUtils.checkTrue(i > 0, ", \n", "") );
                
                List<?> items = childIds.get(i);
                sb.append("[");
                for (int j = 0; j < items.size(); j++) {
                    if (j > 0) sb.append(", \n");
                    sb.append("$$('" + items.get(j) + "')");
                }
                sb.append("]");
            }
            return sb.append("];\n").toString();
        }

        /**
         * <p>
         * 根据页面、版面下布局器，创建子Element对象集合。
         * 将布局器所在的门户结构节点下的所有子节点，逐个填入到布局器的格子中。
         * 如果布局器为循环类型布局器，则把所有子节点填入；否则只把布局器填满为止，多余的子节点将不被展示。
         * </p>
         * @param parent
         * 				布局器所在的门户结构节点
         * @param portNumber
         * @param childIds
         * @return Map
         */
		private Map<String, Element> createChildElements(Node parent, int portNumber, List<List<String>> childIds) {
            int index = -1;
            Map<String, Element> portMappingElement = new HashMap<String, Element>();
            for ( Node child : parent.getChildren() ) {
                index++;
                if (index >= Math.abs(portNumber)) {
                    if (portNumber < 0) {
                        index = 0; // 如果布局器为循环布局器，则重新开始
                    } else {
                        break;     // 如果布局器已经满了则跳出，剩下的子节点将不会被生成放入进来
                    }
                }
 
                Element childElement = new Element((IPageElement) child);
                String portMacro = MacrocodeCompiler.createMacroCode("port" + index); // ${portX}
                
                // 如果portElement已经存在，则说明该index已经put过到childrenMap里。同一index再次被循环到，所以一定是循环类布局器
                if (portMappingElement.get(portMacro) != null) {  
                	// 新增HTML片段对象，直接加入到上一格portElement.htmlFragments中，即两个子节点放同一Element里在同一port里显示。
                	Element portElement = portMappingElement.get(portMacro);
                    portElement.htmlFragments.add(childElement.toHTML());  
                    childIds.get(index).add(child.getId().toString());
                } 
                // 将新生产的Element对象放入childrenMap中
                else {
                    portMappingElement.put(portMacro, childElement);
                    childIds.add( new ArrayList<String>(Arrays.asList(child.getId().toString())) );
                }
            }
 
            // 如果布局器的单元格中还有空的，则加一些【空Element】将其填满
            int filledNum = portMappingElement.size(); // 已经填充的单元格个数
            for ( int i = Math.abs(portNumber) - 1; i >= filledNum; i-- ) {
                final String id = "E" + parent.getId() + "_" + i;
				portMappingElement.put(MacrocodeCompiler.createMacroCode("port" + i), new Element() {
                	public String toHTML() {
                        return "<div id=\"" + id + "\" style=\"display:none\"></div>";
                    }
                }); // 放入空的element
				childIds.add( Arrays.asList(id) );
            }
            
            return portMappingElement;
        }

        /**
         * 将Element对象转换成HTML代码
         */
        public String toHTML() {
            StringBuffer sb = new StringBuffer();
            for ( String htmlFragment : this.htmlFragments ) {
                sb.append(htmlFragment);
            }
            return sb.toString();
        }
        
        public String toString() {
        	return toHTML();
        }
    }
 
    /**
     * 非空值LinkedList对象，所有元素不可能为空
     */
    private class NoNullLinkedList<T> extends LinkedList<T> {
        private static final long serialVersionUID = 1851415859447342905L;
 
        public boolean add(T obj) {
            return EasyUtils.isNullOrEmpty(obj) ? false : super.add(obj);
        }
 
        public boolean addAll(Collection<? extends T> c) {
            for(T obj :  c) {
                this.add(obj);
            }
            return true;
        }
    }
}