package com.boubei.tss.modules.cloud.pay;

public class Result {
	
	private boolean result;
	private String errorMsg;
	private Object data;

	public Result(boolean result, Object obj) {
		this.setResult(result);
		if (result) {
			this.setData(obj);
		} else {
			this.setErrorMsg((String) obj);
		}
	}

	public boolean getResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
