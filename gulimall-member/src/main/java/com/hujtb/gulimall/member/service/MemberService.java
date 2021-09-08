package com.hujtb.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.gulimall.member.entity.MemberEntity;
import com.hujtb.gulimall.member.exception.PhoneExistException;
import com.hujtb.gulimall.member.exception.UserNameExistException;
import com.hujtb.gulimall.member.vo.MemberLoginVo;
import com.hujtb.gulimall.member.vo.MemberRegistVo;
import com.hujtb.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:05:54
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo registVo);

    void checkUsernameUnique(String userName) throws UserNameExistException;

    void checkPhoneUnique(String phone) throws PhoneExistException;

    MemberEntity login(MemberLoginVo loginVo);

    MemberEntity login(SocialUser socialUser);
}

