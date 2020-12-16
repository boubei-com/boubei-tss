package com.boubei.tss.modules.api;

import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.dml.SQLExcutor;

public class BIDataProcess1 extends BIDataProcess {

	protected void handleQuery(SQLExcutor ex, Map<String, String> requestMap) {
		List<Map<String, Object>> list = ex.result;
		for (Map<String, Object> map : list) {
			map.put("helloWorld", true);
		}
	}

}
