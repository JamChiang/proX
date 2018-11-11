package com.pinyougou.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.common.util.IdWorker;
import com.pinyougou.mapper.OrderItemMapper;
import com.pinyougou.mapper.OrderMapper;
import com.pinyougou.mapper.PayLogMapper;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.Cart;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl extends BaseServiceImpl<TbOrder> implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private PayLogMapper payLogMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RedisTemplate redisTemplate;
    //购物车列表在redis中的key的名称
    private static final String CART_LIST = "CART_LIST";


    @Override
    public PageResult search(Integer page, Integer rows, TbOrder order) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbOrder.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(order.get***())){
            criteria.andLike("***", "%" + order.get***() + "%");
        }*/

        List<TbOrder> list = orderMapper.selectByExample(example);
        PageInfo<TbOrder> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public String addOrder(TbOrder order) {
        String outTradeNo = "";
        //1、查询redis中的购物车列表
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps(CART_LIST).get(order.getUserId());
        if (cartList != null && cartList.size() > 0) {

            //支付总金额=本次所有订单的总金额之和
            double totalPayment = 0.0;

            //本次支付的所有订单id字符串；使用,分隔
            String orderIds = "";

            //2、遍历每一个购物车对应生成一个订单
            for (Cart cart : cartList) {
                TbOrder tbOrder = new TbOrder();
                tbOrder.setOrderId(idWorker.nextId());
                //收件人信息
                tbOrder.setReceiverAreaName(order.getReceiverAreaName());
                tbOrder.setReceiver(order.getReceiver());
                tbOrder.setReceiverMobile(order.getReceiverMobile());
                //购买用户
                tbOrder.setUserId(order.getUserId());
                //商家
                tbOrder.setSellerId(cart.getSellerId());

                tbOrder.setCreateTime(new Date());
                tbOrder.setUpdateTime(tbOrder.getCreateTime());

                tbOrder.setSourceType(order.getSourceType());

                //1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭,7、待评价'
                tbOrder.setStatus("1");

                //本笔订单的支付总金额 = 所有订单明细对应的总费用之和
                double orderPayment = 0.0;

                //2.1、遍历每一个商家里面买的那些商品生成订单明细
                for (TbOrderItem orderItem : cart.getOrderItemList()) {
                    orderItem.setId(idWorker.nextId());
                    //设置订单id
                    orderItem.setOrderId(tbOrder.getOrderId());

                    //保存订单明细
                    orderItemMapper.insertSelective(orderItem);

                    //累计订单的总价
                    orderPayment += orderItem.getTotalFee().doubleValue();
                }

                //本笔订单的支付总金额
                tbOrder.setPayment(new BigDecimal(orderPayment));

                //保存订单
                orderMapper.insertSelective(tbOrder);

                //累计本次支付的总金额
                totalPayment += orderPayment;

                //累加订单id
                if (orderIds.length() > 0) {
                    orderIds += "," + tbOrder.getOrderId();
                } else {
                    orderIds = tbOrder.getOrderId() + "";
                }
            }

            //3、如果是微信支付则需要生成支付日志
            if ("1".equals(order.getPaymentType())) {
                TbPayLog payLog = new TbPayLog();
                outTradeNo = idWorker.nextId() + "";
                //支付日志id
                payLog.setOutTradeNo(outTradeNo);
                //未支付
                payLog.setTradeState("0");

                //支付总金额=本次所有订单的总金额之和；一般的电商标准，价格都是长整型的，避免小数点；精确到分
                payLog.setTotalFee((long)(totalPayment*100));

                payLog.setUserId(order.getUserId());
                payLog.setCreateTime(new Date());

                //本次支付的所有订单id字符串；使用,分隔
                payLog.setOrderList(orderIds);

                payLogMapper.insertSelective(payLog);
            }
            //4、删除redis中的购物车列表
            redisTemplate.boundHashOps(CART_LIST).delete(order.getUserId());
        }

        //5、如果是微信支付则返回支付日志id，如果是货到付款则返回空字符串
        return outTradeNo;
    }
}
