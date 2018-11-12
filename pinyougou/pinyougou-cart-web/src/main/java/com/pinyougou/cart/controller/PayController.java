package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/pay")
@RestController
public class PayController {

    @Reference
    private OrderService orderService;

    @Reference
    private WeixinPayService weixinPayService;

    /**
     * 根据订单支付日志id（交易号）到支付系统生成支付订单并返回支付二维码地址
     * @param outTradeNo 订单支付日志id（交易号）
     * @return 包含支付二维码地址的信息
     */
    @GetMapping("/createNative")
    public Map<String, String> createNative(String outTradeNo) {
        //1、获取支付日志
        TbPayLog payLog = orderService.findPayLogByOutTradeNo(outTradeNo);
        if(payLog != null) {
            String totalFee = payLog.getTotalFee() + "";
            //2、调用支付统一下单方法生成支付订单并返回信息
            return weixinPayService.createNative(outTradeNo, totalFee);
        }

        return new HashMap<>();
    }

    /**
     * 根据支付日志id（交易号）查询订单支付状态
     * @param outTradeNo 支付日志id（交易号）
     * @return 操作结果
     */
    @GetMapping("/queryPayStatus")
    public Result queryPayStatus(String outTradeNo){
        Result result = Result.fail("支付失败");
        try {
            //3分钟内查询，每隔3秒
            while(true){
                //1、查询状态
                Map<String, String> resultMap = weixinPayService.queryPayStatus(outTradeNo);

                if (resultMap == null) {
                    break;
                }
                if("SUCCESS".equals(resultMap.get("trade_state"))){
                    //2、支付成功则更新订单状态
                    orderService.updateOrderStatus(outTradeNo, resultMap.get("transaction_id"));

                    result = Result.ok("支付成功");
                    break;
                }

                //如果没有支付则每隔3秒查询
                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
