/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso;

 
public class PWDOperator extends DemoOperator implements IPWDOperator {

    private static final long serialVersionUID = 3790289185993889688L;

    public PWDOperator(Long userId) {
        super(userId);
    }

    public String getPassword() {
        return "123456";
    }

}
