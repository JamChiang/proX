package com.pinyougou.content.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.ContentMapper;
import com.pinyougou.pojo.TbContent;
import com.pinyougou.content.service.ContentService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service(interfaceClass = ContentService.class)
@Transactional
public class ContentServiceImpl extends BaseServiceImpl<TbContent> implements ContentService {

    @Autowired
    private ContentMapper contentMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    public static final String CONTENT = "content";

    @Override
    public void add(TbContent tbContent) {
        super.add(tbContent);
        //更新内容分类的缓存
        updateContentInRedisByCategoryId(tbContent.getCategoryId());
    }

    /**
     * 将redis中分类id对应的缓存数据删除
     * @param categoryId 分类id
     */
    private void updateContentInRedisByCategoryId(Long categoryId) {
        redisTemplate.boundHashOps(CONTENT).delete(categoryId);
    }

    @Override
    public void update(TbContent tbContent) {
        //查询原有的内容
        TbContent oldContent = findOne(tbContent.getId());

        super.update(tbContent);

        //1、根据当前最新内容对应的分类id，将redis中该分类id对应的缓存数据删除
        updateContentInRedisByCategoryId(tbContent.getCategoryId());

        //2、如果修改了内容分类的话；则需要将原来内容分类对应的缓存数据也需要删除
        if (!oldContent.getCategoryId().equals(tbContent.getCategoryId())) {
            updateContentInRedisByCategoryId(oldContent.getCategoryId());
        }

    }

    @Override
    public void deleteByIds(Serializable[] ids) {
        //根据内容id集合查询这些内容对应的内容列表；再遍历每一个内容将其内容分类的缓存数据从redis中删除
        Example example = new Example(TbContent.class);
        example.createCriteria().andIn("id", Arrays.asList(ids));

        List<TbContent> contentList = contentMapper.selectByExample(example);
        if (contentList != null && contentList.size() > 0) {
            for (TbContent content : contentList) {
                updateContentInRedisByCategoryId(content.getCategoryId());
            }
        }

        //再删除内容
        super.deleteByIds(ids);
    }

    @Override
    public PageResult search(Integer page, Integer rows, TbContent content) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbContent.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(content.get***())){
            criteria.andLike("***", "%" + content.get***() + "%");
        }*/

        List<TbContent> list = contentMapper.selectByExample(example);
        PageInfo<TbContent> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public List<TbContent> findContentListByCategoryId(Long categoryId) {
        List<TbContent> contentList = null;

        try {
            //从redis中查询是否存在该分类对应的数据有则返回
            contentList = (List<TbContent>) redisTemplate.boundHashOps(CONTENT).get(categoryId);
            if (contentList != null) {
                return contentList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //根据内容分类（轮播广告）并且有效的内容数据按照排序字段降序排序
        //sql:select * from tb_content where category_id =? and status=1 order by sort_order desc
        Example example = new Example(TbContent.class);

        //分类，状态
        example.createCriteria().andEqualTo("categoryId", categoryId)
                .andEqualTo("status", "1");

        //排序，降序
        example.orderBy("sortOrder").desc();

        contentList = contentMapper.selectByExample(example);

        try {
            //将分类对应的内容设置到redis中
            redisTemplate.boundHashOps(CONTENT).put(categoryId, contentList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contentList;
    }
}
