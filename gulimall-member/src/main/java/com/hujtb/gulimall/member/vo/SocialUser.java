package com.hujtb.gulimall.member.vo;

import lombok.Data;

@Data
public class SocialUser {

    private String accessToken;

    private String remindIn;

    private long expiresIn;

    private String uid;

    private String isRealName;
}
