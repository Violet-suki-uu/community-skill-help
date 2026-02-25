package com.rita.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rita.community.entity.UserEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserEventMapper
 * 作用：数据访问接口，基于 MyBatis-Plus 读写数据库。
 */
@Mapper
public interface UserEventMapper extends BaseMapper<UserEvent> {
}


