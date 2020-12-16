package com.boubei.tss.modules.cloud.pay;

import java.util.Map;

public interface AfterPayService {

    /**
     * @param order_no   商户订单号 CloudOrder.order_no
     * @param real_money 真实付款金额
     * @param payer
     * @param payType    支付类名称
     * @param payClazz   支付类路径
     * @param map
     * @return
     */
    void handle(String order_no, Double real_money, String payer, String payType, String payClazz, Map<?, ?> map);

    /**
     * 退款接口
     *
     * @param order_no      订单号
     * @param refund_fee    退款金额
     * @param out_refund_no 退款单号，可以自己定义，不要重复
     * @param feeType       流水类型 如果账单是充值费的，一般就是退充值费
     * @param toBalance     强制退款到余额
     * @return
     */
    Result refund(String order_no, Double refund_fee, String out_refund_no, String feeType, boolean toBalance);
}
