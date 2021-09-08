package com.hujtb.gulimall.member.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class MemberLoginVo {

    private String loginacct;

    private String password;
}
