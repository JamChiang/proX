package com.pinyougou.user.service;

import com.pinyougou.pojo.TbUser;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

public interface UserService extends BaseService<TbUser> {

    PageResult search(Integer page, Integer rows, TbUser user);

    /**
     * 发送手机短信验证码
     * @param phone 手机号
     */
    void sendSmsCode(String phone);

    /**
     * 在注册的时候根据手机号到redis查询对应的值与用户输入的验证码进行对比；
     * 如果一致则说明校验成功返回true并删除在redis中该手机号存储的验证码；如果不一致则说明校验失败返回false
     * @param phone 手机号
     * @param smsCode 验证码
     * @return 验证结果 true or false
     */
    boolean checkSmsCode(String phone, String smsCode);
}