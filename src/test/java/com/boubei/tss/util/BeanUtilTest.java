/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.cache.CacheStrategy;
import com.boubei.tss.cache.MapContainer;

public class BeanUtilTest {
	
	@Test
	public void test() {
		User obj1 = new User(1, "Jon1");
		obj1.setWeight(66.8);
		
		Map<String, Object> map = new HashMap<String, Object>();
		BeanUtil.addBeanProperties2Map(obj1, map , "id");
		Assert.assertEquals(null, map.get("id"));
		Assert.assertEquals(obj1.getName(), map.get("name"));
		
		User obj2 = new User();
		BeanUtil.copy(obj2, obj1);
		Assert.assertEquals(obj2.getId(), obj1.getId());
		Assert.assertEquals(obj2.getName(), obj1.getName());
		
		User obj3 = new User();
		BeanUtil.copy(obj3, obj1, new String[]{"id"});
		Assert.assertEquals(null, obj3.getId());
		Assert.assertEquals(obj1.getName(), obj3.getName());
		
		Class<?> clazz = BeanUtil.createClassByName("com.boubei.tss.cache.CacheStrategy");
		Assert.assertEquals(CacheStrategy.class, clazz);
		
		Assert.assertFalse(BeanUtil.isImplInterface(clazz, Animal.class));
		
		Assert.assertNotNull(BeanUtil.newInstance(clazz));
		
		Assert.assertNotNull(BeanUtil.newInstanceByName("com.boubei.tss.cache.CacheStrategy"));
		
		BeanUtil.newInstanceByName(MapContainer.class.getName(), new Class[] { String.class }, new Object[] { "XXX" });
		Assert.assertNotNull(BeanUtil.newInstanceByName("com.boubei.tss.cache.CacheStrategy"));
		
		map = BeanUtil.getProperties(obj1);
		Assert.assertEquals(obj1.getId(), map.get("id"));
		Assert.assertEquals(obj1.getName(), map.get("name"));
		Assert.assertEquals(obj1.getId(), BeanUtil.getPropertyValue(obj1, "id"));
		Assert.assertTrue(BeanUtil.isPropertyInBean(obj1, "name"));
		
		Assert.assertFalse(BeanUtil.isPropertyInBean(obj1, "nameX"));
		
		User obj4 = new User();
		map.put("weight", 100);
		BeanUtil.setDataToBean(obj4, map);
		Assert.assertNotNull(obj4.getWeight());
		
		String expectXml = "<com.boubei.tss.util.BeanUtilTest_-User>\n" + 
				  "  <id>1</id>\n" +
				  "  <name>Jon1</name>\n" +
				  "  <weight>100.0</weight>\n" +
				  "  <x>x</x>\n" +
				  "</com.boubei.tss.util.BeanUtilTest_-User>";
		Assert.assertEquals(expectXml, BeanUtil.toXml(obj4));
		
		map = new HashMap<String, Object>();
		map.put("weight", null);
		BeanUtil.setDataToBean(obj4, map, false);
		Assert.assertNull(obj4.getWeight());
		Assert.assertNotNull(obj4.getId());
	}
	
	@Test
	public void testErr() {
		User obj1 = new User(1, "Jon1");
		
		try {
			BeanUtil.getPropertyValue(obj1, "nameX");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("访问属性：nameX时出错".equals(e.getMessage()));
        }
		
		try {
			BeanUtil.createClassByName("com.wrong.X");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("实体: com.wrong.X 无法加载".equals(e.getMessage()));
        }
		
		try {
			BeanUtil.newInstanceByName("com.wrong.X");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("实例化失败：com.wrong.X".equals(e.getMessage()));
        }
		
		try {
			BeanUtil.newInstanceByName("com.boubei.tss.cache.CacheStrategy", null, "1,2".split(","));
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("实例化失败：com.boubei.tss.cache.CacheStrategy".equals(e.getMessage()));
        }
		
		try {
			BeanUtil.newInstance(null);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("实例化失败：null", true);
        }
		
		try {
			BeanUtil.copy(obj1, null);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("拷贝时出错!", true);
        }
		
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("weight", "abc");
			BeanUtil.setDataToBean(obj1, map);
        	Assert.fail();
        } catch (Exception e) {
        	Assert.assertEquals("设置对象User属性【weight】=【abc】出错了，请检查", e.getMessage());
        }
		
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("id", "1.1");
			BeanUtil.setDataToBean(obj1, map);
        	Assert.fail();
        } catch (Exception e) {
        	Assert.assertEquals("设置对象User属性【id】=【1.1】出错了，请检查", e.getMessage());
        }
	}
	
	public interface Animal {
		Integer getId();
		String getName();
	}
	
	public static class User implements Animal {
		Integer id;
		String name;
		Double weight;
		String x = "x";
		
		public User() {}
		
		public User(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
		
		public String toString() {
			return name;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Double getWeight() {
			return weight;
		}

		public void setWeight(Double weight) {
			this.weight = weight;
		}

	}
}
