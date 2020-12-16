/**
 * Copyright (C), 2018-2020, 卜数科技有限公司
 * FileName: ProductTest
 * Author: hank
 * Date: 2020/9/16 下午2:23
 * Description: 为了测试而用的空产品
 */
package com.boubei.tss.modules.cloud;

import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.pay.AbstractProduct;

public class ProductTest extends AbstractProduct {
    public ProductTest() {
    }

    public ProductTest(CloudOrder co) {
        super(co);
    }

    @Override
    protected void beforeOrderModuleCheck() {

    }

    @Override
    protected Double getProxyPrice() {
        return 0D;
    }

    @Override
    public void setPrice() {
    }

    @Override
    protected void handle() {
        log.info("正常执行product付款后操作");

        createFlows(getBuyerAccount());
    }

    @Override
    public String getName() {
        return "测试产品";
    }
}
