package com.alfo.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;


import com.alfo.common.domain.dto.Result;
import com.alfo.common.domain.dto.UserDTO;
import com.alfo.common.utils.UserDTOHolder;
import com.alfo.common.utils.redis.RedisConstants;
import com.alfo.user.domain.po.Follow;
import com.alfo.user.mapper.FollowMapper;
import com.alfo.user.mapper.UserMapper;
import com.alfo.user.service.IFollowService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private FollowMapper followMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //1.判断是否关注
        UserDTO userDTO = UserDTOHolder.getUserDTO();
        Long userId = userDTO.getId();
        String key = RedisConstants.FOLLOWS_KEY + userId;
        Follow follow = new Follow();
        if (!isFollow) {
            //2.关注，取关
            //先删redis，在数据库，保证同时删
            Long delectCount = stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            if (delectCount > 0) {
                followMapper.deleteFollowByIds(followUserId, userId);
                return Result.ok("取关成功");
            }
        }
        //3.未关注，关注
        follow.setFollowUserId(followUserId);
        follow.setUserId(userId);
        Long addCount = stringRedisTemplate.opsForSet().add(key, followUserId.toString());
        if (addCount != null && addCount > 0) {
            followMapper.insertFollow(follow);
            return Result.ok("关注成功");
        }
        return Result.fail("操作失败");
    }

    @Override
    public Result isFollow(Long followUserId) {
        UserDTO userDTO = UserDTOHolder.getUserDTO();
        Long selfId = userDTO.getId();
        //这里直接在Redis中查询即可
        Boolean member = stringRedisTemplate.opsForSet().isMember(RedisConstants.FOLLOWS_KEY + selfId, followUserId.toString());
        return Result.ok(BooleanUtil.isTrue(member));
    }


    @Override
    public Result queryCommonFollow(Long targetUserId) {
        //1.获取当前用户id
        UserDTO userDTO = UserDTOHolder.getUserDTO();
        Long selfUserId = userDTO.getId();
        //2.获取共同关注
        String keyPrefix = RedisConstants.FOLLOWS_KEY;
        Set<String> commonFollowSet = stringRedisTemplate.opsForSet().intersect(keyPrefix + selfUserId, keyPrefix + targetUserId);
        if (commonFollowSet == null || commonFollowSet.isEmpty()) {
            return null;
        }
        List<Long> commonFollowIdList = commonFollowSet.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> commonFollowUser = new ArrayList<>();
        commonFollowIdList.forEach(userId -> {
            commonFollowUser.add(BeanUtil.copyProperties(userMapper.queryUserById(userId), UserDTO.class));
        });
        //3.返回
        return Result.ok(commonFollowUser);
    }
}
