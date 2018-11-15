package com.pinyougou.seckill.service;

import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

public interface SeckillOrderService extends BaseService<TbSeckillOrder> {

    PageResult search(Integer page, Integer rows, TbSeckillOrder seckillOrder);

    /**
     * 生成秒杀订单
     * @param seckillId 秒杀商品id
     * @return 操作结果
     */
    String submitOrder(String userId, Long seckillId) throws InterruptedException;

    /**
     * 根据秒杀订单id查询在redis中的秒杀订单
     * @param outTradeNo 秒杀订单id
     * @return 秒杀订单
     */
    TbSeckillOrder findSeckillOrderInRedisById(String outTradeNo);

    /**
     * 保存秒杀订单到数据库中
     * @param outTradeNo 秒杀订单id
     * @param transaction_id 微信订单号
     */
    void saveSeckillOrderInRedisToDb(String outTradeNo, String transaction_id);

    /**
     * 删除redis中秒杀订单
     * @param outTradeNo 订单Id
     */
    void deleteSeckillOrderInRedis(String outTradeNo) throws InterruptedException;
}