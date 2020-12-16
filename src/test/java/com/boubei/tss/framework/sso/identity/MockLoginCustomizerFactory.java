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

import com.boubei.tss.framework.sso.DoNothingLoginCustomizer;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
 

public class MockLoginCustomizerFactory extends LoginCustomizerFactory {

    public static void init() {
        customizer = new DoNothingLoginCustomizer();
    }
}
