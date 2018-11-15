package com.pinyougou.seckill.service;

import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

import java.util.List;

public interface SeckillGoodsService extends BaseService<TbSeckillGoods> {

    PageResult search(Integer page, Integer rows, TbSeckillGoods seckillGoods);


    /**
     * 查询状态为已审核，剩余库存大于0，大于等于开始时间，小于结束时间的那些秒杀商品并根据开始时间升序排序
     * @return 秒杀商品列表
     */
    List<TbSeckillGoods> findList();

    /**
     * 根据秒杀商品id查询在redis中的秒杀商品
     * @param id 秒杀商品id
     * @return 秒杀商品
     */
    TbSeckillGoods findOneInRedisById(Long id);
}