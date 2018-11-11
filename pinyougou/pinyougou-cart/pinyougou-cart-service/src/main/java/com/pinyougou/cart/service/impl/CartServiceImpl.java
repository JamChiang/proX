package com.pinyougou.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.ItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.vo.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service(interfaceClass = CartService.class)
public class CartServiceImpl implements CartService {

    //购物车列表在redis中的key的名称
    private static final String CART_LIST = "CART_LIST";

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<Cart> addItemToCartList(List<Cart> cartList, Long itemId, Integer num) {

        //1、判断商品是否存在和是否已启用
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        if (item == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!"1".equals(item.getStatus())) {
            throw new RuntimeException("商品非法");
        }

        Cart cart = findCartBySellerId(cartList, item.getSellerId());
        if(cart == null) {
            //2、商品对应的商家（cart）不存在在购物车列表(cartList)；需要重新创建一个商家cart，
            // 然后往其里面的商品列表中添加当前订单商品，并且购买数量要大于0
            if (num > 0) {
                cart = new Cart();
                cart.setSellerId(item.getSellerId());
                cart.setSeller(item.getSeller());

                //订单商品列表
                List<TbOrderItem> orderItemList = new ArrayList<>();

                TbOrderItem orderItem = createOrderItem(item, num);
                orderItemList.add(orderItem);
                cart.setOrderItemList(orderItemList);

                cartList.add(cart);
            } else {
                throw new RuntimeException("商品购买数量非法");
            }
        } else {
            //3、商品对应的商家（cart）存在在购物车列表(cartList)
            TbOrderItem orderItem = findOrderItemByItemId(cart.getOrderItemList(), itemId);

            if(orderItem != null) {
                //3.1、商品存在在商家对应的订单商品列表中则叠加购买商品数量，如果购买数量小于1的话，
                // 需要将该商品从该商家的订单列表中删除；如果该商家一个商品都没有了则需要将该商家从购物车列表中删除
                orderItem.setNum(orderItem.getNum() + num);
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum() * orderItem.getPrice().doubleValue()));
                if(orderItem.getNum() < 1){
                    cart.getOrderItemList().remove(orderItem);
                }
                if (cart.getOrderItemList().size() < 1) {
                    cartList.remove(cart);
                }
            } else {
                //3.2、商品不存在在商家对应的订单商品列表中；则创建一个订单商品并加入该商家的订单列表中
                if (num > 0) {
                    orderItem = createOrderItem(item, num);

                    cart.getOrderItemList().add(orderItem);
                } else {
                    throw new RuntimeException("商品购买数量非法");
                }
            }
        }
        return cartList;
    }

    @Override
    public List<Cart> findCartInRedis(String username) {
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps(CART_LIST).get(username);

        if(cartList != null){
            return cartList;
        }

        return new ArrayList<>();
    }

    @Override
    public void saveCartLisToRedis(String username, List<Cart> cartList) {
        redisTemplate.boundHashOps(CART_LIST).put(username, cartList);
    }

    @Override
    public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
        for (Cart cart : cartList1) {
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                addItemToCartList(cartList2, orderItem.getItemId(), orderItem.getNum());
            }
        }
        return cartList2;
    }

    /**
     * 在订单商品列表中根据商品id查询订单商品
     * @param orderItemList 订单商品列表
     * @param itemId 商品id
     * @return 订单商品
     */
    private TbOrderItem findOrderItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
        if (orderItemList != null && orderItemList.size() > 0) {
            for (TbOrderItem orderItem : orderItemList) {
                if (itemId.equals(orderItem.getItemId())) {
                    return orderItem;
                }
            }
        }
        return null;
    }

    /**
     * 根据商品创建订单商品orderItem
     * @param item 商品sku
     * @param num 购买数量
     * @return orderItem
     */
    private TbOrderItem createOrderItem(TbItem item, Integer num) {
        TbOrderItem orderItem = new TbOrderItem();
        orderItem.setItemId(item.getId());
        orderItem.setPicPath(item.getImage());
        orderItem.setNum(num);
        orderItem.setSellerId(item.getSellerId());
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setTitle(item.getTitle());
        orderItem.setPrice(item.getPrice());

        //这是小计=购买数量*单价
        orderItem.setTotalFee(new BigDecimal(num * orderItem.getPrice().doubleValue()));

        return orderItem;
    }

    /**
     * 根据商家id在购物车列表中查询购物车对象cart
     * @param cartList 购物车列表
     * @param sellerId 商家id
     * @return cart
     */
    private Cart findCartBySellerId(List<Cart> cartList, String sellerId) {
        if (cartList != null && cartList.size() > 0) {
            for (Cart cart : cartList) {
                if (sellerId.equals(cart.getSellerId())) {
                    return cart;
                }
            }
        }
        return null;
    }
}
