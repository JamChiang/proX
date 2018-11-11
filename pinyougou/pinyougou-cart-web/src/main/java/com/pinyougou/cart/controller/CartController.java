package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.common.util.CookieUtils;
import com.pinyougou.vo.Cart;
import com.pinyougou.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/cart")
@RestController
public class CartController {

    //在浏览器中cookie的名称
    private static final String COOKIE_CART_LIST = "PYG_CART_LIST";
    //在浏览器中cookie的生存时间；1天
    private static final int COOKIE_CART_LIST_MAX_AGE = 24*60*60;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Reference
    private CartService cartService;

    /**
     * 登录与未登录情况下实现加入购物车功能
     * @param itemId 商品sku id
     * @param num 购买数量
     * @return 操作结果
     */
    @GetMapping("/addItemToCartList")
    @CrossOrigin(origins = "http://item.pinyougou.com", allowCredentials = "true")
    public Result addItemToCartList(Long itemId, Integer num){
        try {

            //设置允许某个域名的跨域请求响应
            //response.setHeader("Access-Control-Allow-Origin", "http://item.pinyougou.com");
            //设置允许接收客户端cookie和响应cookie
            //response.setHeader("Access-Control-Allow-Credentials", "true");

            //1、获取当前购物车列表
            List<Cart> cartList = findCartList();
            //2、将购买商品sku 的购买数量更新到购物车列表并返回最新的购物车列表
            cartList = cartService.addItemToCartList(cartList, itemId, num);

            //如果在没有登录的时候；则返回的用户名为anonymousUser
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(username)) {
                //3、将最新的购物车列表写回cookie
                //未登录；操作cookie中数据
                CookieUtils.setCookie(request, response, COOKIE_CART_LIST,
                        JSON.toJSONString(cartList), COOKIE_CART_LIST_MAX_AGE, true);
            } else {
                //已登录；操作redis中数据
                cartService.saveCartLisToRedis(username, cartList);
            }
            return Result.ok("加入购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("加入购物车失败");
    }

    /**
     * 在登录或者未登录情况下加载购物车数据
     * @return 购物车列表
     */
    @GetMapping("/findCartList")
    public List<Cart> findCartList(){
        try {
            //如果在没有登录的时候；则返回的用户名为anonymousUser
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            //1、查询cookie中购物车列表；
            List<Cart> cookie_cartList = new ArrayList<>();
            //未登录；则从cookie加载购物车数据
            String cartListJsonStr = CookieUtils.getCookieValue(request, COOKIE_CART_LIST, true);
            if(!StringUtils.isEmpty(cartListJsonStr)){
                cookie_cartList = JSONArray.parseArray(cartListJsonStr, Cart.class);
            }

            if ("anonymousUser".equals(username)) {
                return cookie_cartList;
            } else {
                //已登录；则从redis加载购物车数据
                List<Cart> redis_cartList = cartService.findCartInRedis(username);

                if(cookie_cartList.size() > 0) {
                    //2、合并到redis到一个新的列表
                    redis_cartList = cartService.mergeCartList(cookie_cartList, redis_cartList);

                    //3、删除cookie中的购物车数据
                    CookieUtils.deleteCookie(request, response, COOKIE_CART_LIST);

                    //4、将新的购物车列表写回redis
                    cartService.saveCartLisToRedis(username, redis_cartList);
                }

                return redis_cartList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前登录的用户信息
     * @return 用户信息
     */
    @GetMapping("/getUsername")
    public Map<String, Object> getUsername(){
        Map<String, Object> resultMap = new HashMap<String, Object>();
        //如果在没有登录的时候；则返回的用户名为anonymousUser
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        resultMap.put("username", username);
        return resultMap;
    }
}
