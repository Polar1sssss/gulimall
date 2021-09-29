package com.hujtb.gulimall.cart.vo;

import lombok.Data;

/**
 * 用户信息传输对象，临时用户
 */
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    /**
     * TODO 是否是第一次访问
     */
    private boolean tmpUser = false;
}
