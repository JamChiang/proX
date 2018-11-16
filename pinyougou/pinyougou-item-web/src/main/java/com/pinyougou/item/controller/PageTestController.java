package com.pinyougou.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.sellergoods.service.GoodsService;
import com.pinyougou.sellergoods.service.ItemCatService;
import com.pinyougou.vo.Goods;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RequestMapping("/test")
@RestController
public class PageTestController {

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Reference
    private ItemCatService itemCatService;

    @Reference
    private GoodsService goodsService;

    @Value("${ITEM_HTML_PATH}")
    private String ITEM_HTML_PATH;

    /**
     * 需要根据商品spu id查询商品信息（分类，基本、描述、sku列表）
     * 再获取到freemarker商品item.ftl模版并输出html页面到一个指定路径
     * @param goodsIds 商品spu id数组
     * @return 操作结果
     */
    @GetMapping("/audit")
    public String auditGoods(Long[] goodsIds){

        if (goodsIds != null && goodsIds.length > 0) {
            for (Long goodsId : goodsIds) {
                genHtml(goodsId);
            }
        }

        return "success";
    }

    /**
     * 商品批量删除的时候，根据每个商品spu id到指定路径下将html文件删除
     * @param goodsIds 商品spu id数组
     * @return 操作结果
     */
    @GetMapping("/delete")
    public String deleteGoods(Long[] goodsIds){

        if (goodsIds != null && goodsIds.length > 0) {
            for (Long goodsId : goodsIds) {
                File file = new File(ITEM_HTML_PATH + goodsId + ".html");
                if (file.exists()) {
                    file.delete();
                }
            }
        }

        return "success";
    }

    /**
     * 需要根据商品spu id查询商品信息（分类，基本、描述、sku列表）
     * 再获取到freemarker商品item.ftl模版并输出html页面到一个指定路径
     * @param goodsId 商品spu id
     */
    private void genHtml(Long goodsId) {
        try {
            //获取freemarker配置对象
            Configuration configuration = freeMarkerConfigurer.getConfiguration();

            //1、获取模版
            Template template = configuration.getTemplate("item.ftl");

            //2、数据
            Map<String, Object> dataModel = new HashMap<>();

            //根据商品spu id查询商品信息（基本、描述、sku列表(已启用；并且按照是否默认排序降序排序)）
            //返回的sku列表要按照是否默认降序排序是因为在详情页面刚进入的时候应该要默认显示一个sku
            //Goods goods = goodsService.findGoodsByIdAndStatus(goodsId, "1");

            //根据商品id查询商品基本信息获取3级商品分类id;再根据分类id查询分类
            //itemCat1 1级商品分类中文名称
//            TbItemCat itemCat1 = itemCatService.findOne(goods.getGoods().getCategory1Id());
//            dataModel.put("itemCat1", itemCat1.getName());
//
//            //itemCat2 2级商品分类中文名称
//            TbItemCat itemCat2 = itemCatService.findOne(goods.getGoods().getCategory2Id());
//            dataModel.put("itemCat2", itemCat2.getName());
//
//            //itemCat3 3级商品分类中文名称
//            TbItemCat itemCat3 = itemCatService.findOne(goods.getGoods().getCategory3Id());
//            dataModel.put("itemCat3", itemCat3.getName());
//
//            //goodsDesc 商品描述信息
//            dataModel.put("goodsDesc", goods.getGoodsDesc());
//            //goods 商品基本信息
//            dataModel.put("goods", goods.getGoods());
//            //itemList 商品sku列表
//            dataModel.put("itemList", goods.getItemList());

            //输出媒介
            FileWriter fileWriter = new FileWriter(ITEM_HTML_PATH + goodsId + ".html");

            //3、输出
            template.process(dataModel, fileWriter);

            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
