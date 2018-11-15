package com.pinyougou.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.common.util.IdWorker;
import com.pinyougou.common.util.RedisLock;
import com.pinyougou.mapper.SeckillGoodsMapper;
import com.pinyougou.mapper.SeckillOrderMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.seckill.service.SeckillOrderService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

@Service(interfaceClass = SeckillOrderService.class)
public class SeckillOrderServiceImpl extends BaseServiceImpl<TbSeckillOrder> implements SeckillOrderService {

    //秒杀订单在redis中对应的key的名称
    private static final String SECKILL_ORDERS = "SECKILL_ORDERS";
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    //秒杀商品在redis中对应的key的名称
    private static final String SECKILL_GOODS = "SCEKILL_GOODS";

    @Override
    public PageResult search(Integer page, Integer rows, TbSeckillOrder seckillOrder) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbSeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(seckillOrder.get***())){
            criteria.andLike("***", "%" + seckillOrder.get***() + "%");
        }*/

        List<TbSeckillOrder> list = seckillOrderMapper.selectByExample(example);
        PageInfo<TbSeckillOrder> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public String submitOrder(String userId, Long seckillId) throws InterruptedException {
        String seckillOrderId = "";
        //加分布式锁
        RedisLock redisLock = new RedisLock(redisTemplate);
        if(redisLock.lock(seckillId.toString())) {
            //1、从redis中获取秒杀商品；判断商品是否存在，库存是否大于0
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(SECKILL_GOODS).get(seckillId);
            if (seckillGoods == null) {
                throw new RuntimeException("秒杀商品不存在");
            }
            if (seckillGoods.getStockCount() == 0) {
                throw new RuntimeException("已抢完");
            }
            //2、秒杀商品的库存减1；
            seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
            if(seckillGoods.getStockCount() > 0) {
                //2.1、如果库存大于0，则需要更新秒杀商品到redis
                redisTemplate.boundHashOps(SECKILL_GOODS).put(seckillId, seckillGoods);
            } else {
                //2.2、如果库存等于0，则需要将redis中的秒杀商品更新回mysql；并删除在redis中的秒杀商品
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);

                redisTemplate.boundHashOps(SECKILL_GOODS).delete(seckillId);
            }
            //释放分布式锁
            redisLock.unlock(seckillId.toString());

            //3、生成秒杀订单，并存入redis
            TbSeckillOrder seckillOrder = new TbSeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            seckillOrderId = seckillOrder.getId().toString();
            //未支付
            seckillOrder.setStatus("0");
            seckillOrder.setSeckillId(seckillId);
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setUserId(userId);
            seckillOrder.setSellerId(seckillGoods.getSellerId());
            //秒杀价
            seckillOrder.setMoney(seckillGoods.getCostPrice());

            redisTemplate.boundHashOps(SECKILL_ORDERS).put(seckillOrderId, seckillOrder);
        }
        //4、返回订单id
        return seckillOrderId;
    }

    @Override
    public TbSeckillOrder findSeckillOrderInRedisById(String outTradeNo) {
        return (TbSeckillOrder) redisTemplate.boundHashOps(SECKILL_ORDERS).get(outTradeNo);
    }

    @Override
    public void saveSeckillOrderInRedisToDb(String outTradeNo, String transaction_id) {
        //1、获取秒杀订单
        TbSeckillOrder seckillOrder = findSeckillOrderInRedisById(outTradeNo);
        //2、更新秒杀订单的支付状态
        seckillOrder.setStatus("1");
        seckillOrder.setPayTime(new Date());
        seckillOrder.setTransactionId(transaction_id);
        //3、保存订单到数据库中
        seckillOrderMapper.insertSelective(seckillOrder);

        //4、删除redis中的秒杀订单
        redisTemplate.boundHashOps(SECKILL_ORDERS).delete(outTradeNo);
    }

    @Override
    public void deleteSeckillOrderInRedis(String outTradeNo) throws InterruptedException {

        //1、查询redis中对应的订单
        TbSeckillOrder seckillOrder = findSeckillOrderInRedisById(outTradeNo);
        //加分布式锁
        RedisLock redisLock = new RedisLock(redisTemplate);
        if(redisLock.lock(seckillOrder.getSeckillId().toString())) {
            //2、查询redis中订单对应的秒杀商品
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(SECKILL_GOODS).get(seckillOrder.getSeckillId());

            if (seckillGoods == null) {
                //从Mysql中查询秒杀商品
                seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillOrder.getSeckillId());
            }

            //3、加库存
            seckillGoods.setStockCount(seckillGoods.getStockCount() + 1);

            redisTemplate.boundHashOps(SECKILL_GOODS).put(seckillGoods.getId(), seckillGoods);

            //释放分布式锁
            redisLock.unlock(seckillOrder.getSeckillId().toString());

            //4、删除redis中订单
            redisTemplate.boundHashOps(SECKILL_ORDERS).delete(outTradeNo);
        }

    }
}
