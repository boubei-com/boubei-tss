/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.display.grid;

/** 
 * <p>  Grid数据对象接口。 </p> 
 *
 * 所有需要用Grid展示的实体类都需要实现本接口。
 * 
 */
public interface IGridNode {
    
	/**
	 * 实体类实现本方法时，将需要在Grid里展示的属性放入到GridAttributesMap中
	 * @return
	 */
	GridAttributesMap getAttributes(GridAttributesMap map);
}
