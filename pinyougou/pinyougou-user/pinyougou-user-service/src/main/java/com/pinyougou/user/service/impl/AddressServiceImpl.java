package com.pinyougou.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.AddressMapper;
import com.pinyougou.pojo.TbAddress;
import com.pinyougou.user.service.AddressService;
import com.pinyougou.service.impl.BaseServiceImpl;
import com.pinyougou.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service(interfaceClass = AddressService.class)
public class AddressServiceImpl extends BaseServiceImpl<TbAddress> implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public PageResult search(Integer page, Integer rows, TbAddress address) {
        PageHelper.startPage(page, rows);

        Example example = new Example(TbAddress.class);
        Example.Criteria criteria = example.createCriteria();
        /*if(!StringUtils.isEmpty(address.get***())){
            criteria.andLike("***", "%" + address.get***() + "%");
        }*/

        List<TbAddress> list = addressMapper.selectByExample(example);
        PageInfo<TbAddress> pageInfo = new PageInfo<>(list);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());
    }
}
