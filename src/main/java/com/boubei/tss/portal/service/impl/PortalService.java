/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.PortalDispatcher;
import com.boubei.tss.portal.dao.IComponentDao;
import com.boubei.tss.portal.dao.INavigatorDao;
import com.boubei.tss.portal.dao.IPortalDao;
import com.boubei.tss.portal.engine.PortalGenerator;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.entity.Navigator;
import com.boubei.tss.portal.entity.ReleaseConfig;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.Theme;
import com.boubei.tss.portal.entity.ThemeInfo;
import com.boubei.tss.portal.entity.ThemeInfo.ThemeInfoId;
import com.boubei.tss.portal.helper.ComponentHelper;
import com.boubei.tss.portal.service.IPortalService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
 
@Service("PortalService")
public class PortalService implements IPortalService {

    protected Logger log = Logger.getLogger(this.getClass());
    
    @Autowired private IPortalDao portalDao;
    @Autowired private IComponentDao componentDao;
    @Autowired private INavigatorDao navigatorDao;
    
    //* ******************************************      获取门户结构操作     ******************************************************

    public Structure getStructure(Long id) { 
        return portalDao.getEntity(id); 
    }
   
    public List<?> getAllStructures() { 
        return portalDao.getEntities("from Structure o order by o.decode "); 
    }

    public List<?> getActivePortals() { 
        return portalDao.getEntities("from Structure o where o.disabled <> 1 and o.type = 0 order by o.decode ");
    }

    public List<?> getActivePagesByPortal(Long portalId) { 
        return portalDao.getEntities("from Structure o where o.portalId = ? and o.disabled <> 1" +
                " and o.type in (1,2) order by o.decode ", portalId);
    }

    @SuppressWarnings("unchecked")
    public List<Structure> getStructuresByPortal(Long portalId) { 
        return (List<Structure>) portalDao.getEntities("from Structure o where o.portalId = ? and o.disabled <> 1 " +
                "and o.type <> 0 order by o.decode ", portalId);
        }
    
    
   //* *********************************************   读取组装门户所需数据   ********************************************************
    
    public PortalNode getPortal(Long portalId, Long selectThemeId) {
        Structure portal = portalDao.getEntity(portalId);
        portalDao.evict(portal);
        
        Theme useTheme = portal.getTheme();
        if( selectThemeId != null && selectThemeId > 0) {
        	useTheme = new Theme(selectThemeId);
        }
        portal.setTheme(useTheme);
                
        return getNormalPortal(portal);
    }
    
    /**
     * 缓存池里如果get不到，则生成新的门户对象，返回其clone对象。。
     */
    private PortalNode getNormalPortal(Structure portal) {
        PortalNode portalNode;
        
        String portalCacheKey = portal.getDefaultKey();
        Pool pool = CacheHelper.getLongCache();
        Cacheable item = pool.getObject(portalCacheKey);
        if( item != null ) {
            portalNode = (PortalNode) item.getValue();
        } else {
            portalNode = getPortalNode(portal.getId(), portal.getTheme().getId());
            pool.putObject(portalCacheKey, portalNode);
        }
        
        return (PortalNode) portalNode.clone();
    }
 
    private PortalNode getPortalNode(Long portalId, Long themeId){
        Structure root = portalDao.getEntity(portalId);
        portalDao.evict(root);
        
        List<?> themeInfos = portalDao.getEntities("from ThemeInfo t where t.id.themeId = ?", themeId);
        Map<Long, ThemeInfo> themeInfosMap = new HashMap<Long, ThemeInfo>();
        for(Object temp : themeInfos){
            ThemeInfo info = (ThemeInfo) temp;
            themeInfosMap.put(info.getId().getStructureId(), info);
        }
        
        /* 
         * 根据portalId获取一个完整的门户结构，不包括根节点；
         * 并且根据传入的themeId获取各个门户结构相对应该主题的主题信息，设置到门户结构对象上 
         */
        List<Structure> structuresList = getStructuresByPortal(portalId);
        for( Structure ps : structuresList ) {
            portalDao.evict(ps);
            
            ThemeInfo info = themeInfosMap.get(ps.getId());
            ps.setDecorator(info != null ? info.getDecorator() : componentDao.getDefaultDecorator());
            
            /* 
             * portlet实例: portlet参数取存于门户结构中的参数(ps.getParameters())，修饰器参数取主题中的(info.getParameters())
             * portlet和修饰器的参数分开来放，如果portlet实例的参数也存放于主题信息中的话，那么修改参数将非常麻烦，需要一个个主题修改过来。 
             */
            if(ps.isPortletInstanse()) {                   
                String portletParam = ComponentHelper.getPortletConfig(ps.getParameters());
                String docoratorParam = "<decorator/>" ;
                if( info != null ) { 
                    docoratorParam = ComponentHelper.getDecoratorConfig(info.getParameters());
                }
                ps.setParameters(ComponentHelper.createPortletInstanseConfig(portletParam, docoratorParam));
            } 
            else{
                // 页面和版面的参数取各主题信息的。 页面和版面的布局器和修饰器取默认主题中相应布局器和修饰器
                ps.setDefiner (info != null ? info.getLayout() : componentDao.getDefaultLayout());
                ps.setParameters(info != null ? info.getParameters() : "<params><layout/><portlet/><decorator/></params>");
            }
        }  

        Object[] components = portalDao.getPortalComponents(portalId, themeId);
        PortalNode node = PortalGenerator.genPortalNode(root, structuresList, components);
 
        return node;
    }

    public Structure getStructureWithTheme(Long id) {
        Structure ps = portalDao.getEntity(id);
        if(ps.isRootPortal()) {
            return ps;
        }
        
        // 加载主题信息
        Structure portal = portalDao.getEntity(ps.getPortalId());
        Long currentThemeId = portal.getCurrentTheme().getId();
        ThemeInfo themeInfo = (ThemeInfo) portalDao.getEntity(ThemeInfo.class, new ThemeInfoId(currentThemeId, id));
        if( themeInfo == null ) {
            // 如果该门户结构在当前主题下找不到主题信息，则取默认的修饰和布局
            themeInfo = new ThemeInfo();
            themeInfo.setDecorator(componentDao.getDefaultLayout());
            themeInfo.setLayout(componentDao.getDefaultDecorator());
        }
        
        ps.setDecorator(themeInfo.getDecorator());
        
        // 如果是portlet实例，则取主题的修饰器参数和门户结构上的portlet参数做为新的参数配置。（portlet参数不分主题，只保存在门户结构表中）。
        if( ps.isPortletInstanse() ) {
            String portletParam   = ComponentHelper.getPortletConfig(ps.getParameters());
            String docoratorParam = ComponentHelper.getDecoratorConfig(themeInfo.getParameters());
            ps.setParameters(ComponentHelper.createPortletInstanseConfig(portletParam, docoratorParam));
        } 
        // 只有非portlet实例才直接用主题表中的参数信息（页面、版面的布局修饰器等信息）
        else {
            ps.setDefiner(themeInfo.getLayout());
            ps.setParameters(themeInfo.getParameters());
        }
        
        return ps;
    } 

    public Structure createStructure(Structure ps) {
        if( ps.isRootPortal() ) { 
            // 先为新门户新建一套主题
            Theme theme = new Theme();
            theme.setName(ps.getTheme().getName());
            theme = (Theme) portalDao.createObject(theme);
            
            ps.setTheme(theme);
            ps.setCurrentTheme(theme);
            ps = saveStructure(ps);
            
            Long portalId = ps.getId();
            ps.setPortalId(portalId);
            theme.setPortalId(portalId);
            portalDao.update(theme);
            
            /* 默认新增一个菜单根节点，专门用于新建门户的菜单管理 */
            Navigator portalMenu = new Navigator();
            portalMenu.setType(Navigator.TYPE_MENU);
            portalMenu.setName(ps.getName());
            portalMenu.setPortalId(portalId);
            portalMenu.setParentId(PortalConstants.ROOT_ID);
            portalMenu.setSeqNo(navigatorDao.getNextSeqNo(portalMenu.getParentId()));
            navigatorDao.save(portalMenu);
        }
        else {
            saveStructure(ps);
        }
        
        saveThemeInfo(ps);
        return ps;
    }
    
    public Structure updateStructure(Structure ps) {
        saveStructure(ps);
        saveThemeInfo(ps);
        return ps;
    }

    /**
     * 因为decode生成需要拦截dao的方法（执行前拦截），如果在dao中设置seqNo，会导致拦截时还没有生成seqNo。
     */
    private Structure saveStructure(Structure ps){
        if(ps.getId() == null) {
            ps.setSeqNo(portalDao.getNextSeqNo(ps.getParentId()));
        }
        portalDao.saveStructure(ps);
        
        ps = portalDao.getEntity(ps.getId());
        String code = ps.getCode();
        if(code == null || !code.startsWith("ps-")) {
            ps.setCode("ps-" + EasyUtils.checkNull(ps.getPortalId(), ps.getId()) + "-" + ps.getId());
        }
        portalDao.saveStructure(ps);
        
        return ps;
    }

    /**
     * 保存主题信息。
     * 注：保存门户结构主题信息的同时，门户结构中也保存了最近一次修改的主题信息。
     * @param ps
     */
    private void saveThemeInfo(Structure ps){
        // 门户节点没有主题信息
        if(ps.isRootPortal()) return;
        
        Structure portal = portalDao.getEntity(ps.getPortalId());
        Long currentThemeId = portal.getCurrentTheme().getId();
        
        ThemeInfoId tiId = new ThemeInfoId(currentThemeId, ps.getId());
        
        ThemeInfo info = new ThemeInfo();
		info.setId(tiId);
        info.setDecorator(ps.getDecorator());
        
        if( ps.isPage() || ps.isSection() ) { // 页面、版面
            info.setLayout(ps.getDefiner());
        }
        
        /* 
         * 参数信息即可保存于门户结构上，也可保存于主题信息。
         * 如果考虑主题自定义，则优先使用存于主题上的各组件参数信息。
         */
        info.setParameters(ps.getParameters());
        
        if(portalDao.getEntity(ThemeInfo.class, tiId) != null) {
        	 portalDao.update(info);
        } else {
        	 portalDao.createObject(info);
        }
    }

    public void deleteStructure(Long id) {
        Structure ps = portalDao.getEntity(id);
        
        // 删除一个枝
        List<Structure> branch = portalDao.getChildrenById(id, PortalConstants.PORTAL_DEL_OPERRATION );
        for( Structure node : branch ) {
            portalDao.deleteStructure(node);
            portalDao.executeHQL("delete from ThemeInfo o where o.id.structureId = ?", node.getId());
        }
 
        // 如果删除的是门户：
        if( ps.isRootPortal() ) {
            Long portalId = ps.getPortalId();
            
            // 1、删除门户的菜单信息
            List<?> menus = portalDao.getEntities("from Navigator o where o.portalId=?", portalId);
            for( Object temp : menus ){
                navigatorDao.deleteNavigator((Navigator) temp);
            }
 
            // 2、删除主题信息 
            List<?> themeList = getThemesByPortal(portalId);
            portalDao.deleteAll(themeList);
            
            // 3、则将原门户的上传文件一并删除，删除上传文件前需确保所有逻辑数据已经删除
            FileHelper.deleteFilesInDir("", ps.getPortalResourceFileDir());
        }
    }

    public void disable(Long id, Integer disabled) {
        Structure ps = portalDao.getEntity(id);
        List<Structure> list;
        
        // 如果是启用或者操作的是门户根节点，则处理操作节点以下的所有子节点
        if(disabled.equals(ParamConstants.TRUE) || ps.isRootPortal()){
            list = portalDao.getChildrenById(id, PortalConstants.PORTAL_STOP_OPERRATION );
        }  
        else { // 启用向上
            list = portalDao.getParentsById(id, PortalConstants.PORTAL_START_OPERRATION); 
        }
        
        for( Structure entity : list ){
            entity.setDisabled(disabled);
            portalDao.update(entity);
        }
    }

    public void sort(Long id, Long targetId, int direction) {
        portalDao.sort(id, targetId, direction);
    }
    
    public void move(Long id, Long container) {
    	Structure ps = portalDao.getEntity(id);
    	ps.setSeqNo(portalDao.getNextSeqNo(container));
    	ps.setParentId(container);
    	portalDao.moveEntity(ps);
	}

   /****************************   门户相关的（包括门户主题、门户发布等）的相关维护  *************************************************/
   
    //********************************  以下为主题管理  ***************************************************************
    
    private Theme getTheme(Long id) {
    	return (Theme) portalDao.getEntity(Theme.class, id);
    }
    
    public void specifyDefaultTheme(Long themeId) {
    	Theme theme = getTheme(themeId);
        Structure portal = portalDao.getEntity(theme.getPortalId());
        portal.setTheme(theme);
        portalDao.update(portal);
    }

    public void removeTheme(Long themeId) {
    	Theme theme = getTheme(themeId);
        Structure portal = portalDao.getEntity(theme.getPortalId());
        if(theme.equals(portal.getTheme()) || theme.equals(portal.getCurrentTheme())) {
            throw new BusinessException(EX.P_15);
        }

        portalDao.delete(theme);
    }

    public Theme saveThemeAs(Long themeId, String themeName){
    	Theme theme = getTheme(themeId);
        portalDao.evict(theme);
        theme.setName(themeName);
        theme.setId(null);
        theme = (Theme) portalDao.createObject(theme);

        List<?> list = portalDao.getEntities("from ThemeInfo o where o.id.themeId = ?", themeId);
        for( Object temp : list ){
            ThemeInfo info = (ThemeInfo) temp;
            portalDao.evict(info);
            
            info.getId().setThemeId(theme.getId());
            portalDao.createObject(info);
        }
        return theme;
    }

    public List<?> getThemesByPortal(Long portalId) {
        return portalDao.getThemesByPortal(portalId);
    }

    public void renameTheme(Long themeId, String name) {
        if( EasyUtils.isNullOrEmpty(name) ) {
            throw new BusinessException(EX.P_16);
        }
        
        Theme theme = getTheme(themeId);
        theme.setName(name);
        portalDao.update(theme);
    }

    //********************************  以下为门户发布管理  **************************************************************

    public ReleaseConfig getReleaseConfig(String visitUrl) {
        List<?> list = portalDao.getEntities("from ReleaseConfig o where o.visitUrl = ?", visitUrl);
        if( list.isEmpty() ) {
            throw new BusinessException(EX.P_13);
        }
        return (ReleaseConfig) list.get(0);
    }

    public List<?> getAllReleaseConfigs() {
        return portalDao.getEntities( "from ReleaseConfig o order by o.portal.id " );
    }

    public ReleaseConfig saveReleaseConfig(ReleaseConfig releaseConfig) {
        String visitUrl = releaseConfig.getVisitUrl();
        if( !visitUrl.endsWith(PortalDispatcher.PORTAL_REDIRECT_URL_SUFFIX) ){
            visitUrl += PortalDispatcher.PORTAL_REDIRECT_URL_SUFFIX;
            releaseConfig.setVisitUrl(visitUrl);
        }
        
        List<?> list = portalDao.getEntities("from ReleaseConfig o where o.visitUrl = ?", visitUrl);
        if(releaseConfig.getId() == null) {
            if( list.size() > 0) {
                throw new BusinessException(EX.P_14);
            }
            return (ReleaseConfig) portalDao.createObject(releaseConfig);
        } 
        else {
            if( list.size() > 0) {
                ReleaseConfig temp = (ReleaseConfig) list.get(0);
                if( !temp.getId().equals(releaseConfig.getId()) ) {
                    throw new BusinessException(EX.P_14);
                }
            }
            
            portalDao.update(releaseConfig);
            return releaseConfig ;
        }
    }

    public void removeReleaseConfig(Long id) {
        portalDao.delete(ReleaseConfig.class, id);
    }

    public ReleaseConfig getReleaseConfig(Long id) {
        return (ReleaseConfig) portalDao.getEntity(ReleaseConfig.class, id);
    }
  
    //***********************************  门户流量统计获取 ***************************************************************
    
    public List<?> getFlowRate(Long portalId) {
         List<Object> returnList = new ArrayList<Object>();
         
         String hql = "select p.name, count(f.id) from FlowRate f, Structure p " +
                 "where f.pageId = p.id and p.portalId=? group by p.name,p.decode order by p.decode";
         returnList.addAll(portalDao.getEntities(hql, portalId));
         
         hql = "select '合计', count(f.id) from FlowRate f, Structure p where f.pageId = p.id and p.portalId=?";
         returnList.addAll(portalDao.getEntities(hql, portalId));
         
         return returnList;
     }
}