package com.hujtb.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;


import com.hujtb.common.exception.BizCodeEnum;
import com.hujtb.gulimall.member.exception.PhoneExistException;
import com.hujtb.gulimall.member.exception.UserNameExistException;
import com.hujtb.gulimall.member.feign.CouponFeignService;
import com.hujtb.gulimall.member.vo.MemberLoginVo;
import com.hujtb.gulimall.member.vo.MemberRegistVo;
import com.hujtb.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hujtb.gulimall.member.entity.MemberEntity;
import com.hujtb.gulimall.member.service.MemberService;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.R;


/**
 * 会员
 *
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:05:54
 */
@RestController
@RequestMapping("member/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R test() {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R memberCoupons = couponFeignService.memberCoupons();
        return R.ok().put("member", memberEntity).put("coupons", memberCoupons.get("coupons"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 注册
     *
     * @return
     * @RequestBody：请求体里面的数据是JSON，转成后面的对象
     */
    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo registVo) {
        try {
            memberService.regist(registVo);
        } catch (UserNameExistException e) {
            R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        } catch (PhoneExistException e) {
            R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo loginVo) {
        MemberEntity entity = memberService.login(loginVo);
        if (entity != null) {
            return R.ok().setData(entity);
        } else {
            return R.error(
                    BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMsg()
            );
        }
    }

    /**
     * 社交登录
     * @return
     */
    @PostMapping("/oauth2/login")
    public R oauth2Login(@RequestBody SocialUser socialUser) {
        MemberEntity login = memberService.login(socialUser);
        if (login != null) {
            return R.ok().setData(login);
        } else {
            return R.error(BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
