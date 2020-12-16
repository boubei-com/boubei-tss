/* ==================================================================   
 * Created [2020-09-07] by hank
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */
package com.boubei.tss.modules.cloud.pay;

import com.boubei.tss.modules.cloud.entity.CloudOrder;


public interface IRefund {

    Result refund(CloudOrder co, Double refund_fee, String out_refund_no);
}
