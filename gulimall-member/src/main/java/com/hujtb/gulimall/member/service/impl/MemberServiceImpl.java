package com.hujtb.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hujtb.common.utils.HttpUtils;
import com.hujtb.gulimall.member.dao.MemberLevelDao;
import com.hujtb.gulimall.member.entity.MemberLevelEntity;
import com.hujtb.gulimall.member.exception.PhoneExistException;
import com.hujtb.gulimall.member.exception.UserNameExistException;
import com.hujtb.gulimall.member.service.MemberLevelService;
import com.hujtb.gulimall.member.vo.MemberLoginVo;
import com.hujtb.gulimall.member.vo.MemberRegistVo;
import com.hujtb.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.member.dao.MemberDao;
import com.hujtb.gulimall.member.entity.MemberEntity;
import com.hujtb.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo registVo) {
        MemberEntity memberEntity = new MemberEntity();
        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(levelEntity.getId());

        // 检查用户名和手机号的唯一性，引入异常机制，根据抛出的不同异常，返回不同结果给远程调用方
        checkUsernameUnique(registVo.getUserName());
        checkPhoneUnique(registVo.getPhone());

        memberEntity.setUsername(registVo.getUserName());
        memberEntity.setMobile(registVo.getPhone());
        memberEntity.setNickname(registVo.getUserName());

        // 加密存储，加盐：$1$ + 8位字符
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(registVo.getPassword());
        memberEntity.setPassword(encode);

        // TODO 其他默认信息

        this.baseMapper.insert(memberEntity);
    }

    @Override
    public void checkUsernameUnique(String userName) throws UserNameExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo loginVo) {
        String loginacct = loginVo.getLoginacct();
        String password = loginVo.getPassword();
        MemberEntity entity = this.baseMapper.selectOne(
                new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        if (entity == null) {
            return null;
        }
        // 获取到数据库的password
        String passwordFromDb = entity.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // 密码匹配
        boolean matches = passwordEncoder.matches(password, passwordFromDb);
        if (matches) {
            return entity;
        } else {
            return null;
        }
    }

    /**
     * 社交账号登陆
     * @param socialUser
     * @return
     */
    @Override
    public MemberEntity login(SocialUser socialUser) {
        String uid = socialUser.getUid();
        String accessToken = socialUser.getAccessToken();
        long expiresIn = socialUser.getExpiresIn();
        // 1.判断当前社交用户是否登录过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("uid", uid));
        if (memberEntity != null) {
            // 用户已经注册过
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(accessToken);
            update.setExpiresIn(expiresIn);
            memberDao.updateById(update);

            memberEntity.setAccessToken(accessToken);
            memberEntity.setExpiresIn(expiresIn);
            return memberEntity;
        } else {
            // 没有查询到当前社交帐号对应的记录，需要注册
            MemberEntity regist = new MemberEntity();
            try {
                HashMap<String, String> map = new HashMap<>();
                map.put("access_token", accessToken);
                map.put("uid", uid);
                // 获取第三方用户信息
                HttpResponse response = HttpUtils.doGet("http://api.weibo.com", "/2/users/show.json", "get", new HashMap<String, String>(), map);
                if (response.getStatusLine().getStatusCode() == 200) {
                    // 根据响应结果获取json字符串
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String name = jsonObject.getString("name");
                    String gender = jsonObject.getString("gender");

                    regist.setNickname(name);
                    if (gender != null) {
                        regist.setGender("m".equals(gender) ? 1 : 0);
                    }
                    regist.setSocialUid(uid);
                    regist.setAccessToken(accessToken);
                    regist.setExpiresIn(expiresIn);
                    memberDao.insert(regist);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return regist;
        }
    }

}