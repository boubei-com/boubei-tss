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
 * 虚拟身份认证器：校验用户名和密码
 * </p>
 */
public class PWDUserIdentifier extends BaseUserIdentifier {

    protected IOperator validate() throws UserIdentificationException {
        PWDOperator operator = new PWDOperator(1L);
        
        PasswordPassport passport = new PasswordPassport();
        log.debug(passport.toString());
        
        if(passport.getLoginName().equals(operator.getLoginName()) && 
                passport.getPassword().equals(operator.getPassword()) ) {
            
            log.debug("用户：" + passport.getLoginName() + " 密码验证成功！");
            return operator;
        }
        
        throw new UserIdentificationException("用户/密码不正确，请重新登录");
    }

}
