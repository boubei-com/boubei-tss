/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.identity;

import com.boubei.tss.framework.sso.DemoOperator;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.IPWDOperator;
import com.boubei.tss.framework.sso.IdentityGetter;
import com.boubei.tss.framework.sso.IdentityGetterFactory;
 
public class MockIdentityGetterFactory extends IdentityGetterFactory {

    public static void init() {
        getter = new IdentityGetter(){
                
                public IOperator getOperator(Long standardUserId) {
                    return new DemoOperator(standardUserId);
                }

				public boolean indentify(IPWDOperator operator, String password) {
					return false;
				}
 
            };
    }

}
