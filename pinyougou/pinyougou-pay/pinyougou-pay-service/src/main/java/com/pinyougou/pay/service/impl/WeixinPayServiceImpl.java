package com.pinyougou.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.WXPayUtil;
import com.pinyougou.common.util.HttpClient;
import com.pinyougou.pay.service.WeixinPayService;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeixinPayServiceImpl implements WeixinPayService {

    @Value("${appid}")
    private String appid;

    @Value("${partner}")
    private String partner;
    @Value("${partnerkey}")
    private String partnerkey;

    @Value("${notifyurl}")
    private String notifyurl;

    @Override
    public Map<String, String> createNative(String outTradeNo, String totalFee) {
        Map<String, String> resultMap = new HashMap<>();
        try {
            //1、封装请求参数
            Map<String, String> paramMap = new HashMap<>();
            //公众账号ID
            paramMap.put("appid", appid);
            //商户号
            paramMap.put("mch_id", partner);
            //随机字符串；可以使用微信提供的工具类生成
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            //签名；根据信息提交的时候可以动态生成
            //paramMap.put("sign", "");
            //商品描述
            paramMap.put("body", "我的90期-品优购");
            //商户订单号
            paramMap.put("out_trade_no", outTradeNo);
            //标价金额
            paramMap.put("total_fee", totalFee);
            //终端IP
            paramMap.put("spbill_create_ip", "127.0.0.1");
            //通知地址
            paramMap.put("notify_url", notifyurl);
            //交易类型
            paramMap.put("trade_type", "NATIVE");

            String signedXml = WXPayUtil.generateSignedXml(paramMap, partnerkey);
            System.out.println("提交到微信 统一下单 请求的参数为：" + signedXml);

            //2、提交 统一下单 请求
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(signedXml);
            httpClient.post();

            //3、返回结果
            String content = httpClient.getContent();

            System.out.println("提交到微信 统一下单 返回的数据为：" + content);

            Map<String, String> map = WXPayUtil.xmlToMap(content);

            resultMap.put("outTradeNo", outTradeNo);
            resultMap.put("total_fee", totalFee);
            resultMap.put("result_code", map.get("result_code"));
            resultMap.put("code_url", map.get("code_url"));

            return resultMap;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultMap;

    }

    @Override
    public Map<String, String> queryPayStatus(String outTradeNo) {
        try {
            //1、封装请求参数
            Map<String, String> paramMap = new HashMap<>();
            //公众账号ID
            paramMap.put("appid", appid);
            //商户号
            paramMap.put("mch_id", partner);
            //随机字符串；可以使用微信提供的工具类生成
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            //签名；根据信息提交的时候可以动态生成
            //paramMap.put("sign", "");
            //商户订单号
            paramMap.put("out_trade_no", outTradeNo);


            String signedXml = WXPayUtil.generateSignedXml(paramMap, partnerkey);
            System.out.println("提交到微信 查询订单 请求的参数为：" + signedXml);

            //2、提交 统一下单 请求
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            httpClient.setHttps(true);
            httpClient.setXmlParam(signedXml);
            httpClient.post();

            //3、返回结果
            String content = httpClient.getContent();

            System.out.println("提交到微信 查询订单 返回的数据为：" + content);

            return WXPayUtil.xmlToMap(content);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
