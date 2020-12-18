/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.record.permission.RecordResource;
import com.boubei.tss.dm.record.workflow.WFDefine;
import com.boubei.tss.dm.record.workflow.WFService;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.web.display.tree.LevelTreeParser;
import com.boubei.tss.framework.web.display.tree.StrictLevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.EasyUtils;

@Controller
@RequestMapping("/auth/rc")
public class RecordAction extends BaseActionSupport {
    
    @Autowired private RecordService recordService;
    @Autowired private WFService wfService;
    @Autowired private ICommonService commService;
    
    @RequestMapping("/all")
    public void getAllRecordTree(HttpServletResponse response) {
    	List<Record> result = getPermitedRecords();
        TreeEncoder treeEncoder = new TreeEncoder(result, new LevelTreeParser());
        print("SourceTree", treeEncoder);
    }
 
    private List<Record> getPermitedRecords() {
    	List<Record> result = new ArrayList<Record>();
    	List<Record> all = recordService.getAllRecords();
        List<Record> list1 = recordService.getRecordables(); 
        List<Record> list2 = recordService.getVisiables(); // include record groups
        
        // 过滤权限，有录入权限且状态为启用 或 有浏览权限
        for(Record record : all) {
        	if( list2.contains(record) || (list1.contains(record) && record.isActive()) ){
        		result.add(record);
        	}
        }
        
        return result;
    }
    
    @RequestMapping("/all/json")
    @ResponseBody
    public List<Object> getAllRecords(HttpServletResponse response) {
    	List<Object> result = new ArrayList<Object>();
    	
    	List<Record> list = getPermitedRecords();
    	Map<Object, Object> countMap = wfService.getMyWFCount();
    	for(Record record : list) {
    		if( !record.isActive() ) continue;
    		
			Long id = record.getId();
    		String name = record.getName();
			Long pId = record.getParentId();
			String icon = record.getIcon();
			int wfCount = EasyUtils.obj2Int(countMap.get(id));
			String pageHelp = DMUtil.getExtendAttr(record.getRemark(), "page_help");
			String pageCode = DMUtil.getExtendAttr(record.getRemark(), "page_code");
			String pageUrl  = record.getCustomizePage();
			result.add(new Object[] { id, name, pId, record.getType(), "record", icon, wfCount, pageHelp, pageCode, pageUrl });
    	}
    	
    	return result;
    }
    
    @RequestMapping("/groups")
    public void getAllRecordGroups(HttpServletResponse response) {
        List<?> list = recordService.getAllRecordGroups();
        TreeEncoder treeEncoder = new TreeEncoder(list, new StrictLevelTreeParser(Record.DEFAULT_PARENT_ID));
        treeEncoder.setNeedRootNode(false);
        print("SourceTree", treeEncoder);
    }
    
	@RequestMapping(value = "/id")
    @ResponseBody
    public Object getRecordID( String name ) {
		return recordService.getRecordID(name, Record.TYPE1, false);
    }
    
    @RequestMapping(value = "/detail/{type}")
    public void getRecord(HttpServletRequest request, HttpServletResponse response, @PathVariable("type") int type) {
        String uri = null, defaultTable = null;
        if(Record.TYPE0 == type) {
            uri = DMConstants.XFORM_GROUP;
        } else {
            uri = DMConstants.XFORM_RECORD;
            defaultTable = Record.DEFAULT_TABLE;
        }
        
        XFormEncoder xformEncoder;
        String recordIdValue = request.getParameter("recordId");
        
        if( recordIdValue == null) {
            Map<String, Object> map = new HashMap<String, Object>();
            
            String parentIdValue = request.getParameter("parentId"); 
            if("_root".equals(parentIdValue)) {
            	parentIdValue = null;
            }
            
            Long parentId = parentIdValue == null ? Record.DEFAULT_PARENT_ID : EasyUtils.obj2Long(parentIdValue);
            map.put("parentId", parentId);
            map.put("type", type);
            map.put("table", defaultTable);
            map.put("needLog", ParamConstants.TRUE);
            map.put("needFile", ParamConstants.FALSE);
            map.put("batchImp", ParamConstants.TRUE);
            map.put("showCreator", ParamConstants.TRUE);
            xformEncoder = new XFormEncoder(uri, map);
        } 
        else {
            Long recordId = EasyUtils.obj2Long(recordIdValue);
            Record record = recordService.getRecord(recordId);
            xformEncoder = new XFormEncoder(uri, record);
        }
        
        if( Record.TYPE1 == type ) {
        	DMUtil.setDSList(xformEncoder);
        }
 
        print("SourceInfo", xformEncoder);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void saveRecord(HttpServletResponse response, Record record) {
        boolean isnew = (null == record.getId());
        if(isnew) {
        	recordService.createRecord(record);
        } 
        else {
        	recordService.updateRecord(record);
    		CacheHelper.flushCache(CacheLife.LONG.toString(), "_db_record_" + record.getId());
        }
        
        doAfterSave(isnew, record, "SourceTree");
    }
    
    @RequestMapping(value = "/wf/domain", method = RequestMethod.POST)
    @ResponseBody
    public Object saveWFDef4Domain(Long recordId, String domain, String define) {
    	WFDefine wfDefine = queryWFDef4Domain(recordId, domain);
    	if(wfDefine.getId() != null) {
    		wfDefine.setDefine(define);
    		commService.update(wfDefine);
    	} 
    	else {
    		wfDefine.setId(null);
    		wfDefine.setDefine(define);
    		wfDefine.setDomain(domain);
    		wfDefine.setTableId(recordId);
    		commService.create(wfDefine);
    	}
    	
    	CacheHelper.flushCache(CacheLife.LONG.toString(), "_db_record_" + recordId);
    	return wfDefine;
    }
    
    @RequestMapping(value = "/wf/domain", method = RequestMethod.GET)
    @ResponseBody
    public WFDefine queryWFDef4Domain(Long recordId, String domain) {
    	List<?> list = commService.getList("from WFDefine where tableId = ?1 and domain = ?2 ", recordId, domain);
    	if( list.isEmpty() ) {
    		String def = recordService.getRecord(recordId).getWorkflow();
    		WFDefine wd = new WFDefine();
    		wd.setDefine(def);
    		return wd;
    	}
    	
    	return (WFDefine)  list.get(0);
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
        recordService.delete(id);
        printSuccessMessage();
    }
    
    @RequestMapping(value = "/disable/{id}/{disabled}", method = RequestMethod.POST)
    public void startOrStop(HttpServletResponse response, 
            @PathVariable("id") Long id, @PathVariable("disabled") int disabled) {
        
    	recordService.startOrStop(id, disabled);
        printSuccessMessage();
    }
 
    @RequestMapping(value = "/sort/{startId}/{targetId}/{direction}", method = RequestMethod.POST)
    public void sort(HttpServletResponse response, 
            @PathVariable("startId") Long startId, 
            @PathVariable("targetId") Long targetId, 
            @PathVariable("direction") int direction) {
        
        recordService.sort(startId, targetId, direction);
        printSuccessMessage();
    }
 
    @RequestMapping(value = "/move/{recordId}/{groupId}", method = RequestMethod.POST)
    public void move(HttpServletResponse response, 
            @PathVariable("recordId") Long recordId, @PathVariable("groupId") Long groupId) {
        
        recordService.move(recordId, groupId);
        printSuccessMessage();
    }
    
	@RequestMapping("/operations/{resourceId}")
    public void getOperations(HttpServletResponse response, @PathVariable("resourceId") Long resourceId) {
        List<String> list = PermissionHelper.getInstance().getOperationsByResource(resourceId,
                        RecordPermission.class.getName(), RecordResource.class);

        print("Operation", EasyUtils.list2Str(list));
    }
}
