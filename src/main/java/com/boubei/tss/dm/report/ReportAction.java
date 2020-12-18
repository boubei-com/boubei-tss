/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.permission.ReportPermission;
import com.boubei.tss.dm.report.permission.ReportResource;
import com.boubei.tss.dm.report.timer.ReportJob;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.tree.DefaultTreeNode;
import com.boubei.tss.framework.web.display.tree.ITreeNode;
import com.boubei.tss.framework.web.display.tree.LevelTreeParser;
import com.boubei.tss.framework.web.display.tree.StrictLevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamService;
import com.boubei.tss.modules.timer.JobAction;
import com.boubei.tss.modules.timer.JobDef;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

/**
 * [ {'label':'不要缓存','type':'hidden','name':'noCache','defaultValue':'true'} ]
 */
@Controller
@RequestMapping("/auth/rp")
public class ReportAction extends BaseActionSupport {
    
    @Autowired private ReportService reportService;
    @Autowired private RecordService recordService;
    @Autowired private ICommonService commonService;
    @Autowired private ParamService  paramService;
    
    @RequestMapping("/")
    public void getAllReport(HttpServletResponse response) {
        List<?> list = reportService.getAllReport();
        TreeEncoder treeEncoder = new TreeEncoder(list, new StrictLevelTreeParser(Report.DEFAULT_PARENT_ID));
        print("SourceTree", treeEncoder);
    }
    
	@RequestMapping("/my/ids")
	@ResponseBody
    public List<Long> getMyReportIds() {
        String pt = ReportPermission.class.getName();
		return PermissionHelper.getInstance().getResourceIdsByOperation(pt, Report.OPERATION_VIEW);
    }
	
    @RequestMapping("/my/{groupId}")
	@ResponseBody
    public List<Object> getReportsByGroup(@PathVariable Long groupId) {
    	
    	List<Object> result = new ArrayList<Object>();
    	
    	List<Report> list;
    	if(Report.DEFAULT_PARENT_ID.equals(groupId)) {
    		list = reportService.getAllReport();
    	} else {
    		list = reportService.getReportsByGroup(groupId, Environment.getUserId());
    	}
    	
    	for(Report report : list) {
			if( report.isActive() ) {
				Long id = report.getId();
	    		String name = report.getName();
				Long pid = report.getParentId();
				String icon = report.getIcon();
				String pageHelp = DMUtil.getExtendAttr(report.getRemark(), "page_help");
				String pageCode = DMUtil.getExtendAttr(report.getRemark(), "page_code");
				String pageUrl = null; // report.getDisplayUri();
				result.add(new Object[] { id, name, pid, report.getType(), "report", icon, 0, pageHelp, pageCode, pageUrl}); // 和record的一致
			}
    	}
    	
		return result;
    }
    
    /**
     * 如果指定了分组，则只取该分组下的报表。XML格式 for reporter.html页面
     */
    @RequestMapping("/my")
    public void getMyReports(HttpServletResponse response, Long groupId) {
	    List<Report> list;
	    if(groupId != null) {
	    	list = reportService.getReportsByGroup(groupId, Environment.getUserId());
	    } else {
	    	list = reportService.getAllReport();
	    }
	    
	    List<Report> result = ReportQuery.getMyReports(list, groupId);
	    
        TreeEncoder treeEncoder = new TreeEncoder(result, new LevelTreeParser());
        treeEncoder.setNeedRootNode(false);
        print("SourceTree", treeEncoder);
    }

    @RequestMapping("/template")
    public void getReportTLs(HttpServletResponse response) {
    	StringBuffer sb = new StringBuffer("<actionSet>"); 
    	
    	// pages or more/bi4 等目录下
    	String rtd = DMConstants.getReportTLDir();
 		File reportTLDir = new File(URLUtil.getWebFileUrl(rtd).getPath());
		List<File> files = FileHelper.listFilesByTypeDeeply("html", reportTLDir);
		
		// more/bi_template 下
 		File delfaultDir = new File(URLUtil.getWebFileUrl(DMConstants.REPORT_TL_DIR_DEFAULT).getPath());
		files.addAll(FileHelper.listFilesByTypeDeeply("html", delfaultDir));
		
		int index = 1;
 		for (File file : files) {
			String filePath = file.getPath();
			String treeName = filePath.substring( Math.max(0, filePath.indexOf("/tss/" + rtd)) );
			sb.append("<treeNode id=\"").append(index++).append("\" name=\"").append(treeName).append("\"/>");
		}
 		
 		sb.append("</actionSet>");
        print("SourceTree", sb);
    }
    
    @RequestMapping("/groups")
    public void getAllReportGroups(HttpServletResponse response) {
        List<?> list = reportService.getAllReportGroups();
        TreeEncoder treeEncoder = new TreeEncoder(list, new StrictLevelTreeParser(Report.DEFAULT_PARENT_ID));
        treeEncoder.setNeedRootNode(false);
        print("SourceTree", treeEncoder);
    }
    
    @RequestMapping(value = "/detail/{type}")
    public void getReport(HttpServletRequest request, HttpServletResponse response, @PathVariable("type") int type) {
        String uri = null;
        if(Report.TYPE0 == type) {
            uri = DMConstants.XFORM_GROUP;
        } else {
            uri = DMConstants.XFORM_REPORT;
        }
        
        XFormEncoder xformEncoder;
        String reportIdValue = request.getParameter("reportId");
        
        if( reportIdValue == null) {
            Map<String, Object> map = new HashMap<String, Object>();
            
            String parentIdValue = request.getParameter("parentId"); 
            if("_root".equals(parentIdValue)) {
            	parentIdValue = null;
            }
            
            Long parentId = parentIdValue == null ? Report.DEFAULT_PARENT_ID : EasyUtils.obj2Long(parentIdValue);
            map.put("parentId", parentId);
            map.put("type", type);
            map.put("needLog", ParamConstants.TRUE);
            xformEncoder = new XFormEncoder(uri, map);
        } 
        else {
            Long reportId = EasyUtils.obj2Long(reportIdValue);
            Report report = reportService.getReport(reportId);
            xformEncoder = new XFormEncoder(uri, report);
        }
        
        if( Report.TYPE1 == type ) {
        	DMUtil.setDSList(xformEncoder);
        }
 
        print("SourceInfo", xformEncoder);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void saveReport(HttpServletResponse response, Report report) {
        boolean isnew = (null == report.getId());
        if(isnew) {
        	reportService.createReport(report);
        } else {
        	reportService.updateReport(report);
        }
        
        doAfterSave(isnew, report, "SourceTree");
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
        
    	reportService.delete(id);
        
        // 删除定时JOB，如果有的话
        String jobCode = "ReportJob-" + id;
		List<?> list = commonService.getList("from JobDef where code like ?1", jobCode + "%");
		for( Object obj : list ) {
			commonService.delete(JobDef.class, ((JobDef)obj).getId() );
		}
		
        printSuccessMessage();
    }

    @RequestMapping(value = "/disable/{id}/{disabled}", method = RequestMethod.POST)
    public void startOrStop(HttpServletResponse response, 
            @PathVariable("id") Long id, @PathVariable("disabled") int disabled) {
        
        reportService.startOrStop(id, disabled);
        printSuccessMessage();
    }
    
    // 设置报表是否可订阅
    @RequestMapping(value = "/mailable/{id}/{state}", method = RequestMethod.POST)
    public void subscribe(HttpServletResponse response, @PathVariable("id") Long id, @PathVariable("state") int state) {
        Report report = reportService.getReport(id);
        if( EasyUtils.isNullOrEmpty(report.getScript()) ) {
        	throw new BusinessException(EX.DM_19);
        }
        report.setMailable(state);
        reportService.updateReport(report);
        printSuccessMessage();
    }
 
    @RequestMapping(value = "/sort/{startId}/{targetId}/{direction}", method = RequestMethod.POST)
    public void sort(HttpServletResponse response, 
            @PathVariable("startId") Long startId, 
            @PathVariable("targetId") Long targetId, 
            @PathVariable("direction") int direction) {
        
        reportService.sort(startId, targetId, direction);
        printSuccessMessage();
    }

    @RequestMapping(value = "/copy/{reportId}/{groupId}", method = RequestMethod.POST)
    public void copy(HttpServletResponse response, 
            @PathVariable("reportId") Long reportId, @PathVariable("groupId") Long groupId) {
        
        List<?> result = reportService.copy(reportId, groupId);
        TreeEncoder encoder = new TreeEncoder(result, new LevelTreeParser());
        encoder.setNeedRootNode(false);
        print("SourceTree", encoder);
    }

    @RequestMapping(value = "/move/{reportId}/{groupId}", method = RequestMethod.POST)
    public void move(HttpServletResponse response, 
            @PathVariable("reportId") Long reportId, @PathVariable("groupId") Long groupId) {
        
        reportService.move(reportId, groupId);
        printSuccessMessage();
    }
    
	@RequestMapping("/operations/{resourceId}")
    public void getOperations(HttpServletResponse response, @PathVariable("resourceId") Long resourceId) {
        List<String> list = PermissionHelper.getInstance().getOperationsByResource(resourceId,
                        ReportPermission.class.getName(), ReportResource.class);

        print("Operation", EasyUtils.list2Str(list));
    }
	
	@RequestMapping(value = "/schedule", method = RequestMethod.POST)
    public void saveReportJob(HttpServletResponse response, Long reportId, boolean self, String configVal) {
		
		Report report = reportService.getReport(reportId);
		
		String tag = (self ? "-" + Environment.getUserId() : "");
		String jobName = report.getName() + tag;
		
		// 0 0 12 * * ? | 17:操作货量汇总:boubei@163.com:param1=2018-08-01,param2=today-3
		String configs[] = EasyUtils.split(configVal, "|");
					
		JobDef job = queryReportJob(reportId, self);
		if(job == null) {
			String jobCode = "ReportJob-" + reportId + tag;
			
			job = new JobDef();
			job.setName(jobName);
			job.setCode(jobCode);
			job.setJobClassName (ReportJob.class.getName());
			job.setTimeStrategy (configs[0].trim());
			job.setCustomizeInfo(configs[1].trim());
			commonService.createWithLog(job);
			
			// 可推送的报表自动设置为允许订阅
			if( !self ) {
				report.setMailable(ParamConstants.TRUE);
		        reportService.updateReport(report);
			}
		}
		else {
			job.setName(jobName);
			job.setTimeStrategy (configs[0].trim());
			job.setCustomizeInfo(configs[1].trim());
			commonService.updateWithLog(job);
		}
		new JobAction().refresh();
		
        printSuccessMessage("订阅邮件推送成功");
    }

	@RequestMapping(value = "/schedule", method = RequestMethod.GET)
	@ResponseBody
    public Object[] getReportJob(HttpServletResponse response, Long reportId, boolean self) {
		JobDef job = queryReportJob(reportId, self);
		if(job == null) {
			return null;
		}
		return new Object[] { job.getJobClassName(), job.getTimeStrategy(), job.getCustomizeInfo() };
    }
	
	private JobDef queryReportJob( Long reportId, boolean self) {
		String jobCode = "ReportJob-" + reportId + (self ? "-" + Environment.getUserId() : "");
		List<?> list = commonService.getList("from JobDef where code = ?1", jobCode);
		return (JobDef) (list.isEmpty() ? null : list.get(0));
	}
	
	@RequestMapping(value = "/schedule", method = RequestMethod.DELETE)
	@ResponseBody
    public void delReportJob(HttpServletResponse response, Long reportId, boolean self) {
		JobDef job = queryReportJob(reportId, self);
		if(job != null) {
			commonService.delete(JobDef.class, job.getId());
			new JobAction().refresh();
		}
		printSuccessMessage("成功取消邮件推送");
    }
	
	/** 
	 * 前台下拉列表可用的数据服务 
	 * 所有带script且displayUri为空的report, 后台单独发布的服务（配置到param里）
	 * */
	@RequestMapping("/dataservice")
    public void getDataServiceList(HttpServletResponse response) {
		List<ITreeNode> result = new ArrayList<ITreeNode>();
		
		// 数据表服务
		List<Record> records = recordService.getRecordables();
		for(Record dt : records) {
			String define = dt.getDefine() + "";
			Pattern p = Pattern.compile("name'|'value'", Pattern.CASE_INSENSITIVE); // 忽略大小写
    		Matcher m = p.matcher(define);
        	if( m.find() ) {
        		String nodeName = dt.getName();
        		String url = "/tss/xdata/json/" + dt.getTable() + "/1";
        		try {
        			Record group = recordService.getRecord( dt.getParentId() );
        			nodeName += "(" +group.getName()+ ")";
        		} 
        		catch(Exception e) { }
        		
        		result.add( new DefaultTreeNode(url, nodeName) );
        	}
		}
		
		// 数据服务
        List<?> reports = reportService.getAllReport();
        for(Object temp : reports) {
        	Report report = (Report) temp;
        	String script = report.getScript();
        	if( EasyUtils.isNullOrEmpty(script) ) continue;
        	if( !EasyUtils.isNullOrEmpty(report.getDisplayUri()) ) continue;
        		
        	// 检查参数，数据服务的参数不超过一个;
        	String param = EasyUtils.obj2String( report.getParam() );
        	if( param.indexOf("label") != param.lastIndexOf("label") ) continue;
        	
        	// 检查是否包含了必要的关键字
        	Pattern p = Pattern.compile("text|name|pk", Pattern.CASE_INSENSITIVE); // 忽略大小写
    		Matcher m = p.matcher(script);
        	if( m.find() ) {
        		result.add( new DefaultTreeNode(report.getId(), report.getName()) );
        	}
        }
        
        // 后台Action单独发布的数据服务（配置在param里，eg：/tss/service/xx|XX服务,/tss/service/yy|YY服务）
        Param param = paramService.getParam(PX.DATA_SERVICE_CONFIG);
        if(param != null && param.getValue() != null) {
        	String[] array = param.getValue().split(",");
        	for(String _ds : array) {
        		final String[] ds = _ds.split("\\|");
        		if(ds.length == 2) {
        			result.add( new DefaultTreeNode(ds[0], ds[1]) );
        		}
        	}
        }
        
        TreeEncoder treeEncoder = new TreeEncoder(result);
        treeEncoder.setNeedRootNode(false);
        print("DataServiceList", treeEncoder);
    }
	
	// 报表收藏
	@RequestMapping(value = "/collection/{reportId}/{state}", method = RequestMethod.POST)
	public void collectReport(HttpServletResponse response, 
			@PathVariable("reportId") Long reportId, @PathVariable("state") boolean state) {
		
		String hql = "from ReportUser ru where ru.userId = ?1 and ru.reportId = ?2 and ru.type=1";
		Long userId = Environment.getUserId();
		List<?> list = commonService.getList(hql, userId, reportId);
		ReportUser ru;
		if(list.isEmpty() && state) { // 收藏
			ru = new ReportUser(userId, reportId);
			ru.setType(1);
			commonService.create(ru);
		}
		if( !list.isEmpty() && !state) { // 取消收藏
			ru = (ReportUser) list.get(0);
			commonService.delete(ReportUser.class, (Long) ru.getPK());
		}
		
		printSuccessMessage(state ? "已成功收藏" : "已取消收藏");
	}
	
	// 收藏的报表
	@RequestMapping(value = "/collection", method = RequestMethod.GET)
	@ResponseBody
	public List<?> queryCollectReports() {
		String hql = "select r.id, r.name from Report r, ReportUser ru " +
				" where r.id = ru.reportId and ru.userId = ?1 and ru.type=1 and r.disabled<>1 ";
		return commonService.getList(hql, Environment.getUserId());
	}
	
	// 报表点赞|差评
	@RequestMapping(value = "/zan/{reportId}/{type}", method = RequestMethod.POST)
	public void zanReport(HttpServletResponse response,
			@PathVariable Long reportId, @PathVariable Integer type) {
		
		String hql = "from ReportUser ru where ru.userId=?1 and ru.reportId=?2 and ru.type=?3";
		Long userId = Environment.getUserId();
		List<?> list = commonService.getList(hql, userId, reportId, type);
		if( list.isEmpty() ) {
			ReportUser ru = new ReportUser(userId, reportId);
			ru.setType(type);
			commonService.create(ru);
			printSuccessMessage();
		} 
	}
	
	@RequestMapping(value = "/zan/{reportId}/{type}", method = RequestMethod.GET)
	@ResponseBody
	public Object countZan(@PathVariable Long reportId, @PathVariable Integer type) {
		String hql = "select count(*) from ReportUser ru where ru.reportId=?1 and ru.type=?2";
		return commonService.getList(hql, reportId, type).get(0);
	}
}
