package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.sellergoods.service.GoodsService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.Goods;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Transactional
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl extends BaseServiceImpl<TbGoods> implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsDescMapper goodsDescMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private ItemCatMapper itemCatMapper;

    @Autowired
    private SellerMapper sellerMapper;

    @Override
    public PageResult search(Integer page, Integer rows, TbGoods goods) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
        //过滤掉已删除的数据
        criteria.andNotEqualTo("isDelete", "1");

        if(!StringUtils.isEmpty(goods.getAuditStatus())){
            criteria.andEqualTo("auditStatus", goods.getAuditStatus());
        }
        //根据商家id查询
        if(!StringUtils.isEmpty(goods.getSellerId())){
            criteria.andEqualTo("sellerId", goods.getSellerId());
        }
        if(!StringUtils.isEmpty(goods.getGoodsName())){
            criteria.andLike("goodsName", "%" + goods.getGoodsName() + "%");
        }

        List<TbGoods> list = goodsMapper.selectByExample(example);
        PageInfo<TbGoods> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public void addGoods(Goods goods) {
        //1、保存商品spu基本信息
        //默认未审核
        goods.getGoods().setAuditStatus("0");
        add(goods.getGoods());

        //int i = 1/0;

        //2、保存商品spu描述信息
        //商品spuid在保存完基本信息后会回填；再设置给商品描述信息的主键
        goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
        goodsDescMapper.insertSelective(goods.getGoodsDesc());

        //3、保存商品sku列表
        saveItemList(goods);
    }

    @Override
    public Goods findGoodsById(Long id) {
        Goods goods = new Goods();

        //1、根据商品spu id 查询商品基本信息
        goods.setGoods(findOne(id));

        //2、根据商品spu id 查询商品描述信息
        goods.setGoodsDesc(goodsDescMapper.selectByPrimaryKey(id));

        //3、根据商品spu id 查询商品sku 列表
        //select * from tb_item where goods_id=?
        TbItem param = new TbItem();
        param.setGoodsId(id);
        List<TbItem> itemList = itemMapper.select(param);
        goods.setItemList(itemList);

        return goods;
    }

    @Override
    public void updateGoods(Goods goods) {
        //1、更新商品基本信息
        update(goods.getGoods());

        //2、更新商品描述信息
        goodsDescMapper.updateByPrimaryKeySelective(goods.getGoodsDesc());

        //3、更新商品sku列表
        //3.1、根据商品spu id删除sku列表
        TbItem param = new TbItem();
        param.setGoodsId(goods.getGoods().getId());
        itemMapper.delete(param);

        //3.2、保存最新的sku列表
        saveItemList(goods);
    }

    @Override
    public void updateStatus(Long[] ids, String status) {
        //根据商品spu id集合更新这些商品spu的审核状态
        //要更新的内容
        TbGoods goods = new TbGoods();
        goods.setAuditStatus(status);

        //更新条件
        Example example = new Example(TbGoods.class);
        example.createCriteria().andIn("id", Arrays.asList(ids));

        //update tb_goods set audit_status=? where id in(?,?)
        goodsMapper.updateByExampleSelective(goods, example);

        //如果是审核通过的话；应该将sku商品的状态改为已启动
        if ("2".equals(status)) {
            TbItem item = new TbItem();
            //已启用
            item.setStatus("1");

            Example itemExample = new Example(TbItem.class);
            itemExample.createCriteria().andIn("goodsId", Arrays.asList(ids));

            //update tb_item set status=1 where goods_id in (?,?...)
            itemMapper.updateByExampleSelective(item, itemExample);
        }
    }

    @Override
    public void deleteGoodsByIds(Long[] ids) {
        TbGoods goods = new TbGoods();
        //已删除
        goods.setIsDelete("1");

        Example example = new Example(TbGoods.class);
        example.createCriteria().andIn("id", Arrays.asList(ids));

        //update tb_goods set is_delete='1' where id in(?)
        goodsMapper.updateByExampleSelective(goods, example);
    }

    @Override
    public List<TbItem> findItemListByGoodsIdsAndStatus(Long[] ids, String itemStatus) {
        Example example = new Example(TbItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("goodsId", Arrays.asList(ids)).andEqualTo("status", itemStatus);
        return itemMapper.selectByExample(example);
    }

    /**
     * 保存商品sku列表
     * @param goods 商品信息（基本、描述、sku列表）
     */
    private void saveItemList(Goods goods) {
        if("1".equals(goods.getGoods().getIsEnableSpec())) {
            //启用规格，则需要处理前端传递过滤的sku
            if (goods.getItemList() != null && goods.getItemList().size() > 0) {
                for (TbItem item : goods.getItemList()) {

                    //sku标题 = spu标题+该商品的所有规格选项的值
                    String title = goods.getGoods().getGoodsName();

                    //获取前端传递的规格及其选项
                    Map<String, String> map = JSON.parseObject(item.getSpec(), Map.class);
                    Set<Map.Entry<String, String>> entries = map.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        title += " " + entry.getValue();
                    }

                    item.setTitle(title);

                    setItemValue(item, goods);

                    //保存sku
                    itemMapper.insertSelective(item);
                }
            }
        } else {
            //不启用规格；可以根据spu基本信息生成一条sku商品数据
            TbItem tbItem = new TbItem();

            tbItem.setTitle(goods.getGoods().getGoodsName());
            tbItem.setPrice(goods.getGoods().getPrice());
            //默认库存
            tbItem.setNum(9999);
            //默认不启用
            tbItem.setStatus("0");
            //因为只有一个sku所以默认
            tbItem.setIsDefault("1");
            //设置默认的规格为空
            tbItem.setSpec("{}");

            setItemValue(tbItem, goods);

            itemMapper.insertSelective(tbItem);

        }
    }

    /**
     * 根据商品信息设置sku
     * @param item sku商品
     * @param goods spu商品信息
     */
    private void setItemValue(TbItem item, Goods goods) {
        //根据品牌id查询品牌
        TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
        item.setBrand(brand.getName());

        //从spu中获取第一个图片
        if (!StringUtils.isEmpty(goods.getGoodsDesc().getItemImages())) {
            List<Map> list = JSONArray.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
            item.setImage(list.get(0).get("url").toString());
        }
        item.setGoodsId(goods.getGoods().getId());

        //商品分类；来自spu的第3级分类的中文名称
        TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
        item.setCategoryid(itemCat.getId());
        item.setCategory(itemCat.getName());

        item.setCreateTime(new Date());
        item.setUpdateTime(item.getCreateTime());

        //商家id
        TbSeller seller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
        item.setSellerId(seller.getSellerId());
        item.setSeller(seller.getName());
    }
}
