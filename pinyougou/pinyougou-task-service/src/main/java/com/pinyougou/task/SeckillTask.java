package com.pinyougou.task;

import com.pinyougou.mapper.SeckillGoodsMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 如果在系统中（数据库中的秒杀表）有新的秒杀商品的话应该更新到redis中；
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void refreshSeckillGoods(){

        //查询在redis中的秒杀商品id集合
        Set idsSet = redisTemplate.boundHashOps("SECKILL_GOODS").keys();
        List ids = new ArrayList(idsSet);

        //1、查询已审核、库存大于0，开始时间小于等于当前时间，结束时间大于当前时间并且不在redis中的秒杀商品

        Example example = new Example(TbSeckillGoods.class);

        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("status", "1");
        criteria.andGreaterThan("stockCount", 0);

        criteria.andLessThanOrEqualTo("startTime", new Date());
        criteria.andGreaterThan("endTime", new Date());

        //不在redis中的秒杀商品
        if(ids != null && ids.size() > 0){
            criteria.andNotIn("id", ids);
        }

        example.orderBy("startTime");
        List<TbSeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(example);

        //2、遍历上述的商品一个个地存入redis
        if (seckillGoodsList != null && seckillGoodsList.size() > 0) {
            for (TbSeckillGoods seckillGoods : seckillGoodsList) {
                redisTemplate.boundHashOps("SECKILL_GOODS").put(seckillGoods.getId(), seckillGoods);
            }

            System.out.println("新增缓存了 " + seckillGoodsList.size() + " 个秒杀商品...");
        }
    }

    /**
     * 如果在redis中的秒杀商品过时了的话则应该从redis中移除
     */
    @Scheduled(cron = "0/2 * * * * ?")
    public void removeSeckillGoods(){

        //1、获取在redis中的所有秒杀商品列表
        List<TbSeckillGoods> seckillGoods = redisTemplate.boundHashOps("SECKILL_GOODS").values();

        //2、遍历每个商品，判断结束时间如果小于当前时间则从redis移除并在删除之前同步保存回mysql
        if (seckillGoods != null && seckillGoods.size() > 0) {
            for (TbSeckillGoods seckillGood : seckillGoods) {
                if (seckillGood.getEndTime().getTime() < System.currentTimeMillis()) {

                    //将该商品同步保存回mysql
                    seckillGoodsMapper.updateByPrimaryKeySelective(seckillGood);

                    redisTemplate.boundHashOps("SECKILL_GOODS").delete(seckillGood.getId());

                    System.out.println("从redis中移除了id为：" + seckillGood.getId() + " 的商品...");
                }
            }
        }
    }


}
