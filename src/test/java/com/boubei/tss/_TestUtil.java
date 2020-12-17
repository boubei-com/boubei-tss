/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.Assert;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.modules.log.LogQueryCondition;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MathUtil;
import com.boubei.tss.util.URLUtil;

public class _TestUtil {
	
	protected static Logger log = Logger.getLogger(_TestUtil.class);
	
	static final String PROJECT_NAME = "boubei-tss";
	
    static String dbDriverName = Config.getAttribute("db.connection.driver_class");
    
    public static boolean isH2Database() {
    	return dbDriverName != null && dbDriverName.indexOf("h2") >= 0;
    }
    
    public static boolean isMysqlDatabase() {
        return dbDriverName != null && dbDriverName.indexOf("mysql") >= 0;
    }
    
    public static void assertExMsg(String info, Exception rlt) {
    	Assert.assertTrue( rlt.getMessage().indexOf(info) >= 0 );
    }
    
    public static void mockRequest(HttpServletRequest request, HttpSession session) {
        EasyMock.expect(request.getHeader(RequestContext.USER_REAL_IP)).andReturn("127.0.0.1").times(0, 3);
        EasyMock.expect(request.getAttribute(RequestContext.USER_ORIGN_IP)).andReturn("127.0.0.1");
		EasyMock.expect(request.getRemoteAddr()).andReturn("127.0.0.1");
		
		EasyMock.expect(request.getHeader("Proxy-Client-IP")).andReturn("127.0.0.1").times(0, 3);
		EasyMock.expect(request.getHeader("WL-Proxy-Client-IP")).andReturn("unknown").times(0, 3);
		EasyMock.expect(request.getHeader("HTTP_CLIENT_IP")).andReturn(null).times(0, 3);
		EasyMock.expect(request.getHeader("HTTP_X_FORWARDED_FOR")).andReturn("unknown").times(0, 3);
    }
    
    /**resourceId
     * 通过直接往权限表里插入数据，来虚构授权信息
     */
    public static void mockPermission(String table, String resourceName, Long resourceId, 
    		Long roleId, String operation, int permissionState, int isGrant, int isPass) {
    	
    	Long id = System.currentTimeMillis() - MathUtil.randomInt6();
    	Object[] params = new Object[] { resourceName, resourceId, roleId, operation, permissionState,isGrant,isPass, id };
		SQLExcutor.excuteInsert("insert into " +table+ "(resourcename,resourceId,roleId,operationId,permissionState,isGrant,isPass,id) " +
				"values (?,?,?,?,?,?,?,?)", params , "connectionpool");
    }
	
	public static String getProjectDir() {
        String path = URLUtil.getResourceFileUrl("application.properties").getPath();
        
        int beginIndex = path.startsWith("/") ? 0 : 1; // linux or windows
        return path.substring(beginIndex, path.indexOf("target") - 1 );
    }
	
    public static String getInitSQLDir() {
    	String dbType = "mysql";
    	if( isH2Database() ) {
    		dbType = "h2";
    	}
        return getProjectDir() + "/sql/" + dbType;
    }
    
    public static String getSQLDir() {
        return getProjectDir() + "/sql";
    }
    
    public static void excuteSQL(String sqlDir) {  
    	excuteSQL(sqlDir, true);
    }
    
    public static void excuteSQL(String sqlDir, boolean isTSS) {  
        log.info("正在执行目录：" + sqlDir+ "下的SQL脚本。。。。。。");  
        
        Pool connePool = JCache.getInstance().getConnectionPool();
		Cacheable connItem = connePool.checkOut(0);
		
        try {  
        	Connection conn = (Connection) connItem.getValue();
            Statement stmt = conn.createStatement();  
            
            List<File> sqlFiles = FileHelper.listFilesByTypeDeeply(".sql", new File(sqlDir));
            for(File sqlFile : sqlFiles) {
            	String fileName = sqlFile.getName();
				if(isTSS && "roleusermapping-init.sql".equals(fileName)) {
                    continue; 
                }
            	
            	log.info("开始执行SQL脚本：" + fileName+ "。");  
            	
                String sqls = FileHelper.readFile(sqlFile, "UTF-8");
                String[] sqlArray = sqls.split(";");
                for(String sql : sqlArray) {
                	if( EasyUtils.isNullOrEmpty(sql) ) continue;
                	
                	log.debug(sql);  
                	stmt.execute(sql);
                }
				
                log.info("SQL脚本：" + fileName+ " 执行完毕。");  
            }
 
            log.info("成功执行目录：" + sqlDir+ "下的SQL脚本!");
            stmt.close(); 
            
        } catch (Exception e) {  
            throw new RuntimeException("目录：" + sqlDir+ "下的SQL脚本执行出错：", e);
        } finally {
        	connePool.checkIn(connItem);
        }
    }
    
    public static int printLogs(LogService logService) {
        try {
            Thread.sleep(1*1000); // 休息一秒，等日志生成
        } catch (InterruptedException e) {
        } 
        
        LogQueryCondition condition = new LogQueryCondition();
        
        PageInfo result = logService.getLogsByCondition(condition);
        List<?> logs = result.getItems();
        Integer logCount = (Integer) result.getTotalRows();
        
        log.debug("本次测试共生成了 " + logCount + " 条日志");
        
        return logs.size();
    }
    
    public static List<?> printEntity(IDao<?> dao, String entity) {
        List<?> list = dao.getEntities("from " + entity );
        
        log.debug("表【" + entity + "】的所有记录:");
        for(Object temp : list) {
            log.debug(temp);
        }
        log.debug("\n");
        
        return list;
    }
}
