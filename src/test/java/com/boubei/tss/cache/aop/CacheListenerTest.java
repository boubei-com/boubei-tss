package com.boubei.tss.cache.aop;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.Config;
import com.boubei.tss.modules.cloud.entity.DomainConfig;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class CacheListenerTest extends AbstractTest4F {

    @Test
    public void test() {
        DomainConfig e = new DomainConfig();
        e.setId(null);
        e.setCode("DC1");
        e.setContent("1");
        e.setRemark("test");
        e.setUser("Admin");
        e.setDomain("xx");
        EasyUtils.obj2Json(e);

        Assert.assertTrue(e._isTrue());

        e.setContent("");
        Assert.assertNull(e.getContent());

        CacheListener.afterEntityModify(e);

        Config.setProperty("DomainConfigCacheClear", "##SHORT|11#SHORT|getDomain(${domain})");
        CacheListener.afterEntityModify(e);
    }

    @Test
    public void test2() {
        DomainConfig e = new DomainConfig();
        e.setId(null);
        e.setCode("DC1");
        e.setContent("1");
        e.setRemark("test");
        e.setUser("Admin");
        e.setDomain("xx");
        EasyUtils.obj2Json(e);

        DomainConfig e2 = new DomainConfig();
        BeanUtil.copy(e2, e);
        e2.setDomain("xx@--");

        Config.setProperty("DomainConfigCacheClear", "SHORT|11#SHORT|getDomain(${domain})");
        CacheListener.afterEntityModify(e2, e);

		e2.setDomain("xx");
		Config.setProperty("DomainConfigCacheClear", "SHORT|11#SHORT|getDomain(${domain})");
		CacheListener.afterEntityModify(e2, e);
		
		CacheListener.afterEntityModify( new User() );
    }

}
