package com.hujtb.gulimall.authserver.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class UserLoginVo {

    @NotEmpty(message = "请输入用户名")
    @Length(min = 6, max = 20, message = "用户名长度必须为6-20位字符")
    private String loginacct;

    @NotEmpty(message = "请输入密码")
    @Length(min = 6, max = 20, message = "密码长度必须为6-20位字符")
    private String password;
}
