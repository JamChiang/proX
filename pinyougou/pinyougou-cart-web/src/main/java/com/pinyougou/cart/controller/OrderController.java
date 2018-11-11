package com.pinyougou.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.vo.PageResult;
import com.pinyougou.vo.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/order")
@RestController
public class OrderController {

    @Reference
    private OrderService orderService;

    @RequestMapping("/findAll")
    public List<TbOrder> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult findPage(@RequestParam(value = "page", defaultValue = "1")Integer page,
                               @RequestParam(value = "rows", defaultValue = "10")Integer rows) {
        return orderService.findPage(page, rows);
    }

    /**
     * 保存订单信息（基本、明细、支付日志）到数据库
     * @param order 订单基本信息
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody TbOrder order) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            //下单的用户
            order.setUserId(userId);
            //来源；pc
            order.setSourceType("2");

            String ourtTradeNo = orderService.addOrder(order);

            return Result.ok(ourtTradeNo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("提交订单失败");
    }

    @GetMapping("/findOne")
    public TbOrder findOne(Long id) {
        return orderService.findOne(id);
    }

    @PostMapping("/update")
    public Result update(@RequestBody TbOrder order) {
        try {
            orderService.update(order);
            return Result.ok("修改成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/delete")
    public Result delete(Long[] ids) {
        try {
            orderService.deleteByIds(ids);
            return Result.ok("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail("删除失败");
    }

    /**
     * 分页查询列表
     * @param order 查询条件
     * @param page 页号
     * @param rows 每页大小
     * @return
     */
    @PostMapping("/search")
    public PageResult search(@RequestBody  TbOrder order, @RequestParam(value = "page", defaultValue = "1")Integer page,
                               @RequestParam(value = "rows", defaultValue = "10")Integer rows) {
        return orderService.search(page, rows, order);
    }

}
