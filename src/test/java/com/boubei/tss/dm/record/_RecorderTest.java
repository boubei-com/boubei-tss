/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class _RecorderTest extends AbstractTest4DM {
	
	@Autowired LogService logService;
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	
	Long recordId;
	String recordName;
	
	private void initTable(String tableName) {
		super.init();
		
		String tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false', 'options':{'codes':'1|2','names':'物流仓|资金仓'} }," +
        		"	{'label':'名称', 'code':'f2', 'type':'string'}," +
        		"	{'label':'时间', 'code':'f3', 'type':'datetime', 'nullable':'false'}," +
        		"	{'label':'附件', 'code':'f4', 'type':'file'}," +
        		"	{'label':'XX', 'type':'hidden'}" +
        		"]";
		
		Record record = new Record();
		record.setName("record" + tableName);
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable(tableName);
		record.setDefine(tblDefine);
		
		record.setNeedLog(ParamConstants.TRUE);
		record.setNeedFile(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		recordId = record.getId();
		recordName = record.getName();
	}

	@Test
	public void test1() {
		initTable("x_tbl_12");
		runTest();
	}
	
	@Test
	public void test2() {
		initTable("x_tbl_13");
		
		SecurityUtil.LEVEL_6 = 2;
		runTest();
		SecurityUtil.LEVEL_6 = 6;
	}
	
	public void runTest() {
		Assert.assertNotNull(recorder.getDefine(request, recordId));
		
		Assert.assertNotNull( recorder.prepareParams(request, -100L) );
		
		request.addParameter("f1", "10.9");
		request.addParameter("f2", "just test");
		
		// test 更新时必填字段为空
		try {
			recorder.create(request, response, recordId);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) { }
		try {
			recorder.createAndReturnID(request, recordId);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) { }
				
		request.addParameter("f3", "2015-04-05");
		recorder.create(request, response, recordId);
		recorder.createAndReturnID(request, recordId);
		recorder.update(request, response, recordId, 1L);
		
		recorder.getImportTL(response, recordId);
		
		// test 更新时必填字段为空，只update有值的自动
		request.removeParameter("f3");
		request.addHeader(RequestContext.REQUEST_TYPE, RequestContext.XMLHTTP_REQUEST);
		recorder.update(request, response, recordId, 1L);
		
		List<Map<String, Object>> result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.size() == 2);
		
		request = new MockHttpServletRequest();
		request.addParameter("f2", "just test"); // test as query condition
		recorder.showAsGrid(request, response, recordId, 1);
		recorder.showAsJSON(request, recordId, 1);
		
		request.addParameter("rows", "100"); // test as query condition by page
		recorder.showAsJSON(request, recordId, 1);
		
		request.addParameter("recordName", recordName);
		recorder.showAsJSON(request, "-111");
		
		request.removeParameter("recordName");
		String rName = "X9";
		try {
			recorder.showAsJSON(request, rName);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.DM_14, rName) , e.getMessage());
        }
		
		try {
			recorder.get(request, recordId, -999L);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.DM_13B, -999L) , e.getMessage());
        }
		
		response = new MockHttpServletResponse();
		recorder.export(request, response, recordId);
		
		// test query
		Map<String, String> params = new HashMap<String, String>();
		params.put(_Field.STRICT_QUERY, "false");
		params.put("f1", "10.9");
		params.put("f2", "just"); // 支持模糊查询
		result = recorder.getDB(recordId).select(1, 100, params).result;
		Assert.assertTrue(result.size() == 2);
		Object item1ID = result.get(0).get("id");
		
		request = new MockHttpServletRequest();
		Map<String, Object> m = recorder.get(request, recordId, EasyUtils.obj2Long(item1ID));
		Assert.assertNotNull(m.get("f1"));
		
		request = new MockHttpServletRequest();
		try {
			recorder.get(request, recordId, -999L);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.DM_13B, -999L) , e.getMessage());
        }
		
		params = new HashMap<String, String>();
		
		params.put("id", item1ID.toString()); 
		result = recorder.getDB(recordId).select(1, 100, params).result;
		Assert.assertTrue(result.size() == 1);
		
		params.put("id", "-1"); // 支持模糊查询
		params.put("f2", "");
		result = recorder.getDB(recordId).select(1, 100, params).result;
		Assert.assertTrue(result.size() == 0);
		
		for(int i = 0; i < 17; i++) {
			request = new MockHttpServletRequest();
			request.addParameter("f1", "12.0");
			request.addParameter("f2", "i'm " + i);
			request.addParameter("f3", "2015-04-05");
			request.addParameter("_version", String.valueOf(i + 2));
			recorder.update(request, response, recordId, 1L); // 多次修改，以生成日志
		}
		
		recorder.updateBatch(request, response, recordId, "1,2", "f1", "1212");
		result = recorder.getDB(recordId).select(1, 100, null).result;
		Map<String, Object> recordItem = result.get(0);
		Assert.assertTrue(recordItem.get("f1").equals(1212.0d));
		
		try {
			recorder.updateBatch(request, response, recordId, "1,2", "fxx1", "1212");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.DM_32, "fxx1"), e.getMessage());
        }
		
		// test 乐观锁version
		try {
			request = new MockHttpServletRequest();
			request.addParameter("_version", "1");
			recorder.update(request, response, recordId, 1L);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("修改异常，该记录在你修改期间已经被其它人修改过了，请关闭刷新记录，再重新打开进行修改。", true);
        }
		
		// test update after error, then rollback
		try {
			request = new MockHttpServletRequest();
			request.addParameter("_after_", "[{ \"sqlCode\": \"s24\", \"data\": {} }]");
			recorder.update(request, response, recordId, 1L);
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue(e.getMessage().indexOf("code = s24 SQLDef not exsit") >= 0 );
	    }
		
		// test upload record attach
		Long itemId = EasyUtils.obj2Long(recordItem.get("id"));
		uploadDocFile(recordId, itemId);
		List<?> attachList = recorder.getAttachList(request, recordId, itemId);
		Assert.assertTrue(attachList.size() == 1);
		
		RecordAttach ra = (RecordAttach) attachList.get(0);
		ra.setHitCount(ra.getHitCount());
		ra.setFileSize(ra.getFileSize());
		ra.setOrigin(ra.getOrigin());
		log.info(ra.toString());
		Assert.assertEquals("123.txt", ra.getName());
		Assert.assertEquals(recordId, ra.getRecordId());
		Assert.assertEquals(itemId, ra.getItemId());
		Assert.assertNotNull(ra.getSeqNo());
		Assert.assertEquals("txt", ra.getFileExt());
		Assert.assertNotNull(ra.getFileName());
		Assert.assertNotNull(ra.getUploadDate());
		Assert.assertTrue(ra.isOfficeDoc());
		Assert.assertFalse(ra.isImage());
		
		recorder.showAsGrid(request, response, recordId, 1);
		
		// test set top
		RecordAttach ra2 = new RecordAttach();
		BeanUtil.copy(ra2, ra, "id,seqNo".split(","));
		ra2.setSeqNo(2);
		commonDao.create(ra2);
		Assert.assertEquals(2, ra2.getSeqNo().intValue());
		
		attachList = recorder.getAttachList(request, recordId, itemId);
		Assert.assertTrue(attachList.size() == 2);
		recorder.setTopAttach( ra2.getId() );
		ra = (RecordAttach) commonDao.getEntity(RecordAttach.class, ra.getId());
		ra2 = (RecordAttach) commonDao.getEntity(RecordAttach.class, ra2.getId());
		Assert.assertEquals(2, ra.getSeqNo().intValue());
		Assert.assertEquals(1, ra2.getSeqNo().intValue());
		
		recorder.sortAttach(ra2.getId() + ",999," + ra.getId());
		
		try {
			recorder.downloadAttach(request, new MockHttpServletResponse(), ra.getId(), null);
			request.addParameter("smaller", "true");
			recorder.downloadAttach(request, new MockHttpServletResponse(), ra.getId(), "true");
			recorder.downloadPicSL(request, new MockHttpServletResponse(), ra.getId());
			
			recorder.downloadAttach(request, new MockHttpServletResponse(), -1010L, "true");
		} catch (Exception e1) {
			Assert.assertTrue("下载附件出错。", true);
		}
		
		recorder.getAttachListXML(request, response, recordId, itemId);
		
		// test clone
		Long cloneId = recorder.clone(request, recordId, itemId);
		Assert.assertEquals( recorder.get(request, recordId, itemId).get("f1"), recorder.get(request, recordId, cloneId).get("f1") );
		List<?> attachList2 = recorder.getAttachList(request, recordId, cloneId);
		Assert.assertEquals(2, attachList2.size());
		recorder.deleteAttach(request, response, ((RecordAttach)attachList2.get(0)).getId());
		recorder.delete(request, response, recordId, cloneId);
		
		// test delete attach
		request.addParameter("recordId", ra.getRecordId().toString());
		recorder.deleteAttach(request, response, ra.getId());
		try {
			recorder.deleteAttach(request, response, ra.getId());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("该附件找不到了，可能已被删除!", true);
        }
		
		attachList = recorder.getAttachList(request, recordId, itemId);
		Assert.assertTrue(attachList.size() == 1);
		
		// test delete record
		commonDao.executeHQL("update Record set logicDel=1 where id = ?1 ", recordId);
		CacheHelper.flushCache(CacheLife.LONG.toString(), "_db_record_" + recordId);
		
		recorder.delete(request, response, recordId, 1L);
		recorder.delete(request, response, recordId, 2L);
		recorder.delete(request, response, recordId, 2L); // 两次逻辑删除 == 物理删除
		
		result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.size() == 1); // Admin能看到逻辑删除的数据
		
		Map<String, String> params2 = new HashMap<String, String>();
		params2.put("domain", EasyUtils.obj2String(Environment.getDomainOrign()) + _Database.deletedTag);
		result = recorder.getDB(recordId).select(1, 100, params2 ).result;
		Assert.assertTrue(result.size() == 1);
		
		recorder.restore(request, response, recordId, 1L); // 还原后在物理删除
		commonDao.executeHQL("update Record set logicDel=0 where id = ?1 ", recordId);
		CacheHelper.flushCache(CacheLife.LONG.toString(), "_db_record_" + recordId);
		
		recorder.delete(request, response, recordId, 1L);
		
		result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.size() == 0);
		
		try { Thread.sleep(1000); } catch (Exception e) { } // 等待修改日志输出
		assertTrue(_TestUtil.printLogs(logService) > 0);
		
		try {
			request = new MockHttpServletRequest();
			request.addParameter("f1", "12.13");
			request.addParameter("f2", "just test end");
			request.addParameter("f3", "2015-04-05");
			recorder.update(request, response, recordId, 1L);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("修改出错，该记录不存在，可能已经被删除。", true);
        }
		
		// test delete batch
		for(int i=2; i < 6; i++) {
			uploadDocFile(recordId, i);
			
			request.removeAllParameters();
			request.addParameter("_tempID", "" + i);
			request.addParameter("f1", i +".9");
			request.addParameter("f2", "just test");	
			request.addParameter("f3", "2015-04-05");
			request.addParameter("f4", "123.txt#" +ra.getId()+ ",好.png"); 
			
			if(i == 2) {
				request.addParameter("remainAttachs", "1,2");
			}
			
			recorder.create(request, response, recordId);
		}
		recorder.showAsGrid(request, response, recordId, 1);
		
		result = recorder.getDB(recordId).select(1, 100, null).result;
		List<Long> ids = new ArrayList<Long>();
		for(Map<String, Object> item : result) {
			Long _itemId = EasyUtils.obj2Long(item.get("id"));
			uploadDocFile(recordName, _itemId);
			ids.add(_itemId);
		}
		
		recorder.deleteBatch(request, response, recordId, EasyUtils.list2Str(ids).replaceFirst(",", ",  ,"));
		try {
			recorder.deleteBatchII(request, response, recordId, "1,2");
		} catch(Exception e) {
//			Assert.assertEquals("您对此数据记录【1】没有维护权限，无法修改或删除。", e.getMessage());
		}
		
		result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.isEmpty());
		
		Assert.assertTrue( _TestUtil.printEntity(super.permissionHelper, "RecordAttach").isEmpty() ); 
		
		// test not need file
		Record record = recordService.getRecord(recordId);
		record.setNeedFile(ParamConstants.FALSE);
		recordService.updateRecord(record);
		CacheHelper.getLongCache().flush();
		
		recorder.showAsGrid(request, response, recordId, 1);
		try{
			recorder.delete(request, response, recordId, -1L);
			if( SecurityUtil.isHardMode() ) {
				Assert.fail("should throw exception but didn't.");
			}
	    } catch (Exception e) {
	    	Assert.assertTrue("没有权限，因记录不存在", true);
	    }
		
		// test 透析
		request.addParameter("fields", "f3");
		request.addParameter("groupby", "f1");
		recorder.showAsGrid(request, response, recordId, 1);
		
		Assert.assertTrue( !SecurityUtil.isHardestMode() );
	}
}
