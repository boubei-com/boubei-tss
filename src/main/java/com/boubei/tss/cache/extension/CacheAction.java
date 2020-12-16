/* ==================================================================   
 * Created [2015-9-12] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache.extension;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.AbstractPool;
import com.boubei.tss.cache.CacheStrategy;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.framework.web.display.grid.DefaultGridNode;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.framework.web.display.tree.DefaultTreeNode;
import com.boubei.tss.framework.web.display.tree.ITreeNode;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.param.ParamService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boubei.tss.util.XMLDocUtil;

/**
 * TODO 设置 /cache/ 的资源路径，只有Admin能访问
 */
@Controller
@RequestMapping("/cache")
public class CacheAction extends BaseActionSupport {
	
	protected Logger log = Logger.getLogger(this.getClass());
    
    /** 缓存策略模板目录 */
    final static String CACHESTRATEGY_XFORM_TEMPLET = "template/cache/strategy_xform.xml";
    final static String POOLS_GRID_TEMPLET = "template/cache/pool_grid.xml";
 
    private static JCache cache = JCache.getInstance();
    
    @Autowired public ParamService paramService;
    
    /**
     * 一般改的是池大小及等待时间等，只需更新pool对应的策略对象，无需重新生成pool对象。
     */
    @RequestMapping(method = RequestMethod.POST)
    public void modifyCacheConfig(HttpServletResponse response, String cacheCode, String jsonData) {
		
		// 将更新信息保存到系统参数模块(ParamListener-->PCache将会执行 rebuildCache)
		Param cacheGroup = CacheHelper.getCacheParamGroup(paramService);
		
		List<Param> cacheParams = paramService.getParamsByParentCode(PX.CACHE_PARAM);
		Map<String, Param> map = new HashMap<>();
		for(Param temp : cacheParams) {
			map.put(temp.getCode(), temp);
		}
		
		Param cacheParam = map.get(cacheCode);
		if(cacheParam == null) { // 新建一个对象池Param配置
			Long parentId = cacheGroup.getId();
			String name = cache.getPool(cacheCode).getCacheStrategy().getName();
			cacheParam = ParamManager.addSimpleParam(parentId, cacheCode, name, jsonData);
		}
		else {
			cacheParam.setValue(jsonData);
	        paramService.saveParam(cacheParam);
		}
        
        printSuccessMessage();
    }
 
    /**
     * 树型展示所有缓存池
     */
    @RequestMapping("/list")
    public void getAllCacheStrategy4Tree(HttpServletResponse response) {
        List<CacheStrategy> strategyList = new ArrayList<CacheStrategy>();
        
        Set<Entry<String, Pool>> pools = cache.listCachePools(); 
        for(Entry<String, Pool> entry : pools) {
            Pool pool = entry.getValue(); 
            strategyList.add(pool.getCacheStrategy());
        }
        
        List<ITreeNode> treeNodeList = new ArrayList<ITreeNode>();
        for(final CacheStrategy stategy : strategyList) {
        	DefaultTreeNode node = new DefaultTreeNode(stategy.code, stategy.name);
        	node.getAttributes().put("icon", "images/cache.gif");
        	node.getAttributes().put("display", stategy.visible);
        	
            treeNodeList.add(node);
        }
        
        TreeEncoder encoder = new TreeEncoder(treeNodeList);
        encoder.setNeedRootNode(false);
        print("CacheTree", encoder);
    }
    
    @RequestMapping("/grid")
    public void getPoolsGrid(HttpServletResponse response) {
    	
    	List<IGridNode> dataList = new ArrayList<IGridNode>();
    	 
    	Set<Entry<String, Pool>> pools = cache.listCachePools(); 
        for(Entry<String, Pool> entry : pools) {
            Pool pool = entry.getValue(); 
            CacheStrategy strategy = pool.getCacheStrategy();
            
            DefaultGridNode gridNode = new DefaultGridNode();
            Map<String, Object> attrs = gridNode.getAttrs();
			attrs.put("code", entry.getKey());
            attrs.put("name", pool.getName());
            attrs.put("accessMethod", strategy.getAccessMethod());
            attrs.put("disabled", strategy.getDisabled());
            attrs.put("cyclelife", strategy.getCyclelife() / 1000);
            attrs.put("interruptTime", strategy.getInterruptTime());
            attrs.put("poolSize", strategy.getPoolSize());
            attrs.put("initNum", strategy.getInitNum());
            attrs.put("requests", pool.getRequests());
            attrs.put("hitrate", Math.round( pool.getHitRate() ) + "%");
            attrs.put("hitLong", Math.round( pool.getHitLong() / Math.max(1, pool.getRequests()) ));
            
        	AbstractPool _pool = (AbstractPool)pool;
        	int busys = _pool.getUsing().size();
			attrs.put("freeItemNum", _pool.getFree().size());
			attrs.put("busyItemNum", EasyUtils.checkTrue(busys > 0, "<b>" + busys + "</b>", busys));
            
            dataList.add(gridNode);
        }
        
        GridDataEncoder gEncoder = new GridDataEncoder(dataList, POOLS_GRID_TEMPLET);
        print("PoolGrid", gEncoder);
    }
    
    /**
     * 获取缓存策略以及缓存池信息
     */
    @RequestMapping("/list/{code}")
    public void listCacheItems(HttpServletResponse response, @PathVariable String code) {
        Pool pool = cache.getPool(code);
        CacheStrategy strategy = pool.getCacheStrategy();
        Map<String, Object> strategyProperties = new HashMap<String, Object>();
        BeanUtil.addBeanProperties2Map(strategy, strategyProperties);
        
        XFormEncoder xEncoder = new XFormEncoder(CACHESTRATEGY_XFORM_TEMPLET, strategyProperties); 
        String hitRate = Math.round(pool.getHitRate()) + "%";
        
        Set<Cacheable> cachedItems = pool.listItems();
        long requests = strategy.getPoolInstance().getRequests();
        List<IGridNode> dataList = new ArrayList<IGridNode>();
        for(Cacheable item : cachedItems) {
            int hit = item.getHit();
            Object thisKey = item.getKey();
            int hitrate = MathUtil.calPercent(hit, requests);
            long birthday = item.getBirthday();
            long now = System.currentTimeMillis();
            
            DefaultGridNode gridNode = new DefaultGridNode();
            gridNode.getAttrs().put("id", thisKey);
            gridNode.getAttrs().put("key", thisKey.toString());
            gridNode.getAttrs().put("code", code);
            gridNode.getAttrs().put("death", item.getDeath());
			gridNode.getAttrs().put("birthday", DateUtil.formatCare2Second(new Date(birthday)) );
			gridNode.getAttrs().put("age", (now - birthday)/1000 );
            gridNode.getAttrs().put("hit", new Integer(hit));
			gridNode.getAttrs().put("hitRate", hitrate + "%");
            gridNode.getAttrs().put("remark", item.getValue());
            
        	boolean isFree = ( (AbstractPool)pool ).getFree().keySet().contains(thisKey);
        	if( !isFree ) {
        		gridNode.getAttrs().put("state", "1");
        		gridNode.getAttrs().put("hitLong", (now - item.getAccessed()) / 1000);
        	} 
            
            dataList.add(gridNode);
        }
        
        StringBuffer template = new StringBuffer();
        template.append("<grid><declare sequence=\"true\">");
        template.append("<column name=\"id\" mode=\"string\" display=\"none\"/>");
        template.append("<column name=\"code\" mode=\"string\" display=\"none\"/>");
        template.append("<column name=\"key\" caption=\"key\" mode=\"string\" width=\"300px\" sortable=\"true\"/>");
        template.append("<column name=\"birthday\" caption=\"出生时间\" mode=\"string\" width=\"100px\"/>");
        template.append("<column name=\"age\" caption=\"已存活(秒)\" mode=\"string\" width=\"60px\" sortable=\"true\"/>");
        template.append("<column name=\"hitLong\" caption=\"当前占时(秒)\" mode=\"string\" width=\"70px\" sortable=\"true\"/>");
        template.append("<column name=\"hit\" caption=\"命中次数\" mode=\"string\" width=\"60px\" sortable=\"true\"/>");
        template.append("<column name=\"hitRate\" caption=\"命中率\" mode=\"string\" width=\"50px\"/>");
        template.append("<column name=\"state\" caption=\"状态 \" mode=\"string\" width=\"50px\" values=\"0|1\" texts=\"空闲|忙碌\"/>");
        template.append("<column name=\"remark\" caption=\"说明\" mode=\"string\" width=\"120px\"/>");
        template.append("</declare><data></data></grid>");
        
        GridDataEncoder gEncoder = new GridDataEncoder(dataList, XMLDocUtil.dataXml2Doc(template.toString()));
           
        int totalRows = cachedItems.size();
        String pageInfo = generatePageInfo(totalRows, 1, totalRows + 1, totalRows); // 加入分页信息，总是只有一页。
        print(new String[]{"CacheStrategy", "CacheItemList", "PageInfo", "HitRate"}, 
                new Object[]{xEncoder, gEncoder, pageInfo, hitRate});
    }
    
    /**
     * 查看详细的缓存项内容。对象XML格式展示
     */
    @RequestMapping("/item/{code}")
    public void viewCachedItem(HttpServletResponse response, 
    		@PathVariable String code, 
    		@RequestParam("key") String key) {
    	
        Cacheable item = cache.getPool(code).getObject(key);
        if(item != null) {
            print( EasyUtils.obj2String(item.getValue()) );
        }
        else {
            print( EX.CACHE_5 );
        }
    }
    
    @RequestMapping(value = "/item/{code}", method = RequestMethod.DELETE)
    public void removeCachedItem(HttpServletResponse response, 
    		@PathVariable String code, 
    		@RequestParam("key") String key) {
    	
        Pool pool = cache.getPool(code);
		boolean rt = pool.destroyByKey(key);
        printSuccessMessage( !rt ? "destroy succeed。" : EX.CACHE_5);
    }
    
    /**
     * 清空释放缓存池
     */
    @RequestMapping("/release/{code}")
    public void releaseCache(HttpServletResponse response, @PathVariable String code){
        cache.getPool(code).flush();
        printSuccessMessage();
    }
    
    /**
     * 初始化缓存池
     */
    @RequestMapping("/init/{code}")
    public void initPool(HttpServletResponse response, @PathVariable String code){
        cache.getPool(code).init();
        printSuccessMessage();
    }
}

