package com.alfo.user.service;


import com.alfo.common.domain.dto.Result;

import com.alfo.user.domain.po.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long id, Boolean isFollow);

    Result isFollow(Long id);

    Result queryCommonFollow(Long targetUserId);
}
