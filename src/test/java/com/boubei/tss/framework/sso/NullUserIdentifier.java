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

import com.boubei.tss.framework.exception.UserIdentificationException;
import com.boubei.tss.framework.sso.identifier.BaseUserIdentifier;

/** 
 * <p>
 * 虚拟身份认证器：以匿名身份登录
 * </p>
 */
public class NullUserIdentifier extends BaseUserIdentifier {

    protected IOperator validate() throws UserIdentificationException {
        return null;
    }

}
