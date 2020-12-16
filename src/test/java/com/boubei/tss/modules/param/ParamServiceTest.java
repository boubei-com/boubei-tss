/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.param;

import static org.junit.Assert.assertTrue;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.sso.DemoOperator;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;

public class ParamServiceTest extends AbstractTest4F {
    
    /** 导入application.properties文件 */
    @Test
    public void testImportApplicationProperties(){
        ResourceBundle resources = ResourceBundle.getBundle("application", Locale.getDefault());
        if (resources == null) return;
        
        Param group = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "系统参数");
        for (Enumeration<String> enumer = resources.getKeys(); enumer.hasMoreElements();) {
            String key = enumer.nextElement();
            if("class.name.LoginCostomizer,email.default".indexOf(key) >= 0) continue;
            
            String value = resources.getString(key);
            ParamManager.addSimpleParam(group.getId(), key, key, value);
        }
        
        // test Param Manager
        String testCode = "application.code";
		Assert.assertEquals("TSS", ParamManager.getSimpleParam(testCode).getValue());
        Assert.assertEquals("TSS", ParamManager.getValue(testCode));
        Assert.assertEquals("TSS", ParamManager.getValueNoSpring(testCode));
        
        Assert.assertEquals("TSS", ParamManager.getValue(testCode, "WMS"));
        Assert.assertEquals("WMS", ParamManager.getValue("no-exsits-param", "WMS"));
        
        try {
        	ParamManager.getValueNoSpring("not-exsits");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.F_10, "not-exsits"), e.getMessage());
        }
        
        Assert.assertEquals("TSS", paramService.getParam(testCode).getValue());
        printParams();
        
        String token = TokenUtil.createToken(new Random().toString(), 120L); 
        Context.initIdentityInfo( new IdentityCard(token, new DemoOperator(120L)) );
        try {
        	paramService.delete(group.getId());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("您不能执行当前操作，请联系系统管理员！", true); // 没有权限，只有Admin或创建人本人能删除
        }
        
        token = TokenUtil.createToken(new Random().toString(), 12L); 
        Context.initIdentityInfo( new IdentityCard(token, new DemoOperator(12L)) );
        paramService.delete(group.getId());
    }
    
    private void printParams() {
        List<?> list = paramService.getAllParams(false);
        assertTrue(list.size() > 0);
        
        for(Object temp :list) {
            Param p = (Param) temp;
            log.debug(p.getAttributes4XForm());
        }
    }
    
    /** CRUD/排序/移动/复制/停用启用等  */
    @Test
    public void testParamFunction() {
        Param paramGroup = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "测试参数组1");
        String comboParamCode = "book";
		Param comboParam = ParamManager.addComboParam(paramGroup.getId(), comboParamCode, "可选书籍");
        
		ParamManager.addParamItem(comboParam.getId(), "Thinking in JAVA", "Thinking in JAVA", ParamConstants.COMBO_PARAM_MODE);
		ParamManager.addParamItem(comboParam.getId(), "Effictive JAVA", "Effictive JAVA", ParamConstants.COMBO_PARAM_MODE);
		ParamManager.addParamItem(comboParam.getId(), "Design Pattern", "设计模式", ParamConstants.COMBO_PARAM_MODE);
        
        Param paramGroup2 = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "测试参数组2");
        String treeParamCode = "group";
		Param treeParam = ParamManager.addTreeParam(paramGroup2.getId(), treeParamCode, "组织");
        
        Param temp = ParamManager.addParamItem(treeParam.getId(), "group1", "组一", ParamConstants.TREE_PARAM_MODE);
        ParamManager.addParamItem(temp.getId(), "group2", "组二", ParamConstants.TREE_PARAM_MODE);
        ParamManager.addParamItem(treeParam.getId(), "group3", "组三", ParamConstants.TREE_PARAM_MODE);
        
        printParams();
        List<Param> treeList = ParamManager.getTreeParam(treeParamCode);
        paramService.startOrStop(treeParam.getId(), 1);
        paramService.startOrStop(treeParam.getId(), 0);
        
        paramService.sortParam(paramGroup2.getId(), paramGroup.getId(), -1);
        paramService.copyParam(treeParam.getId(), paramGroup.getId());
        
        printParams();
        
        // test Param Manager
        List<Param> list = ParamManager.getComboParam(comboParamCode);
        Assert.assertEquals(3, list.size());
        
        Assert.assertNotNull( ParamManager.getComboParamItem(comboParamCode, "Design Pattern") );
        Assert.assertNotNull( ParamManager.getComboParamItem(comboParamCode, "设计模式") );
        Assert.assertNull( ParamManager.getComboParamItem(comboParamCode, "not exsits") );
        
        list = ParamManager.getTreeParam(treeParamCode);
        Assert.assertEquals(0, list.size());  // 停用父节点时，子节点参数项都已被停用；启用父节点不启子节点
        
        for(Param tp : treeList) {
        	paramService.startOrStop(tp.getId(), 0);
        }
        list = ParamManager.getTreeParam(treeParamCode);
        Assert.assertEquals(3, list.size());  // 都已重新启用
        
        Param simpleParam = ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, "test1", "test1", "test1");
        Assert.assertEquals("test1", simpleParam.getName());
        
        simpleParam.setName(null);
        simpleParam.setHidden(1);
        simpleParam.setUdf1("udf1");
        simpleParam.setUdf2("udf2");
        simpleParam.setUdf3("udf3");
        paramService.saveParam(simpleParam);
        Assert.assertEquals("test1", simpleParam.getCode());
        
        try {
        	ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, "test1", "test1", "test1");
        } catch(Exception e) {
        	log.debug(e.getMessage());
        }
    }
}
