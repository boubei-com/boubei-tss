/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.boubei.tss.cms.helper.ArticleHelperTest;
import com.boubei.tss.cms.job.ArticlePublishTest;
import com.boubei.tss.cms.job.CMSJobsTest;
import com.boubei.tss.cms.lucene.IndexHelperTest;
import com.boubei.tss.cms.module.ArticleModuleTest;
import com.boubei.tss.cms.module.ChannelModuleTest;
import com.boubei.tss.util.ImgUtilTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ArticleModuleTest.class,
	ChannelModuleTest.class,
	IndexHelperTest.class,
	ArticlePublishTest.class,
	CMSJobsTest.class,
	ArticleHelperTest.class,
	ImgUtilTest.class,
})
public class _AllCMSTests {
 
}
