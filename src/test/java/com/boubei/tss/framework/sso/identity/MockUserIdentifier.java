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

import com.boubei.tss.framework.exception.UserIdentificationException;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.identifier.BaseUserIdentifier;
 

public class MockUserIdentifier extends BaseUserIdentifier {
 
    protected IOperator validate() throws UserIdentificationException {
        throw new UserIdentificationException();
    }

}
