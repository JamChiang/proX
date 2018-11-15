package com.pinyougou.pay.service;

import java.util.Map;

public interface WeixinPayService {
    /**
     * 根据订单支付日志id（交易号）到支付系统生成支付订单并返回支付二维码地址
     * @param outTradeNo 订单支付日志id（交易号）
     * @param totalFee 订单支付总金额
     * @return 包含支付二维码地址的信息
     */
    Map<String, String> createNative(String outTradeNo, String totalFee);

    /**
     * 根据支付日志id（交易号）查询订单支付状态
     * @param outTradeNo 支付日志id（交易号）
     * @return 查询结果
     */
    Map<String, String> queryPayStatus(String outTradeNo);

    /**
     * 关闭订单
     * @param outTradeNo 订单号
     * @return 操作结果
     */
    Map<String, String> closeOrder(String outTradeNo);
}
