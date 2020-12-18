package com.boubei.tss.modules.sn;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * TODO 加号子缓存池，一次取N个先缓存着
 */
class SNCreator {
	
	ICommonService commonService = Global.getCommonService();
	
	private String domain;     // "无域" 则为全局取号器
	private String snTemplate; // 取号模板
	private int length = 0 ;   // 固定长度
 
	SNCreator(String domain, String snTemplate, int length) { 
		this.domain = domain;
		this.snTemplate = snTemplate;
		this.length = length;
	}
	
	public String toString() {
		return "取号器：" + snTemplate + ", " + domain;
	}

	// 取单个号
	public String create() {
		return this.create( 1 ).get(0);
	}
	
	/**
	 * 批量取号
	 * 
	 * @param domain
	 * @param count 批量获取单号个数
	 */
	public List<String> create( int count ) {
		
		snTemplate = EasyUtils.obj2String(snTemplate).toLowerCase();
		if( !snTemplate.endsWith(_Field.SNO_xxxx) ) {  // eg: SO、ASN等
			snTemplate += _Field.SNO_yyMMddxxxx;
		}
		
		String precode = snTemplate.replace(_Field.SNO_yyMMddxxxx, "").replace(_Field.SNO_xxxx, "").toUpperCase();
		
		// 如果域扩展表(x_domain)里明确维护了订单前缀
		String prefix = EasyUtils.obj2String( SQLExcutor.queryVL("select prefix from x_domain where domain = ?", "prefix", domain) ); // domain=无域，则不会查到记录
		precode = prefix + precode;
		
		List<String> result = new ArrayList<String>();
		
		// 优先从SelfNO里出（tag = 取号人）
		String hql = " from SelfNO where tag = ?1 and (used = 0 or used is null) and domain = ?2 order by id ";
		String userCode = Environment.getNotnullUserCode();
		List<?> list = commonService.getList(hql, userCode, domain);
		if( list.size() >= count ) {
			for(int i = 0; i < count; i++) {
				SelfNO sn = (SelfNO) list.get(i);
				sn.setUsed(ParamConstants.TRUE);
				commonService.update(sn);
				
				result.add( sn.getCode() );
			}
			return result;
		}
		
		Date _day = DateUtil.parse("2018-08-23");
		String snMode = "";
		if( snTemplate.endsWith(_Field.SNO_yyMMddxxxx) ) {
			_day = DateUtil.today();
			snMode = DateUtil.format(_day, "yyyyMMdd").substring(2);
		} 
				
		// 全局锁
		synchronized( this ) {
			int lastNum;
			Object snID;
			List<Map<String, Object>> l = SQLExcutor.queryL("select id, lastNum from x_serialno where day = ? and precode = ? and domain = ?", _day, precode, domain);
			if( l.isEmpty() ) {
				lastNum = 0;
				snID = SQLExcutor.excuteInsert("insert into x_serialno(day,precode,lastNum,domain,creator,createtime,version) values(?,?,?,?,?,?,?)", 
						new Object[] { _day, precode, 0, domain, Environment.getUserCode(), new Date(), 0}, 
						DMConstants.LOCAL_CONN_POOL);
			} 
			else {
				Map<String, Object> row = l.get(0);
				lastNum = (int) row.get("lastnum");
				snID = row.get("id");
			}
			
			count = Math.min(Math.max(1, count), 100000); // 单次最多1到100000个
			for(int i = 1; i <= count; i++) {
				int currNum = lastNum + i;
				
				String sn = genSN(currNum, precode, snMode);
				result.add(sn);
			}
			
			// 改用SQL 修改，避免多线程 导致乐观锁（其它Service调用取号器，调用完事务不一定马上提交，所以出了（return）本方法，取号器就要实时更新掉）
			SQLExcutor.excute("update x_serialno set lastNum = lastNum + " + count + " where id = " + snID, DMConstants.LOCAL_CONN_POOL);
		}
		
		return result;
	}
	
	String genSN(int currNum, String precode, String snMode) {
		if( !EasyUtils.isNullOrEmpty(snMode) || length == 0 ) {
			String sn = "00" + currNum;
			sn = sn.substring(sn.length() - (currNum >= 1000 ? String.valueOf(currNum).length() : 3));
			sn = precode + snMode + sn;
			return sn;
		}
		
		char[] nums = String.valueOf(currNum).toCharArray();
		length = Math.max(length, precode.length() + Math.max(nums.length, 3));
		char[] arr = new char[length];
		for(int i = 0; i < arr.length; i++) {
			arr[i] = '0';
		}
		for(int i = 0; i < precode.length(); i++) {
			arr[i] = precode.charAt(i);
		}
		for(int i = 0; i < nums.length; i++) {
			arr[ arr.length - nums.length + i] = nums[i];
		}
		
		return new String(arr);
	}
}
