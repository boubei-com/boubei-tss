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

import com.boubei.tss.framework.sso.online.CacheOnlineUserManager;
import com.boubei.tss.framework.sso.online.OnlineUserManagerFactory;

public class MockOnlineUserManagerFactory extends OnlineUserManagerFactory {

    public static void init() {
        manager = new CacheOnlineUserManager();
    }
}
