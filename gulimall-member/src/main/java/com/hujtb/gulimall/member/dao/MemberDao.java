package com.hujtb.gulimall.member.dao;

import com.hujtb.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author hujtb
 * @email hujtb@qq.com
 * @date 2021-07-08 18:05:54
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
