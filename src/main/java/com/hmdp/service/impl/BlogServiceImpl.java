package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserDTOHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FollowMapper followMapper;

    @Override
    public Result queryBlogById(Long id) {
        //1.查询blog
        Blog blog = blogMapper.queryBlogById(id);
        //1.1如果blog为空返回错误信息
        if (blog == null) {
            return Result.fail("该笔记不存在");
        }
        //2填充blog对象中的用户信息
        queryBlogUser(blog);
        //3.返回
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void queryBlogUser(Blog blog) {
        User user = userMapper.queryUserById(blog.getUserId());
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }

    private void isBlogLiked(Blog blog) {
        UserDTO userDTO = UserDTOHolder.getUserDTO();
        if (userDTO == null) {   //如果未登录，不在判断是否点赞
            return;
        }
        Long userId = userDTO.getId();
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();  //ZSCORE key value
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));
        Boolean liked = (score != null);
        if (BooleanUtil.isTrue(liked)) {
            blog.setIsLike(true);
        }
    }

    @Override
    public Result likeBlog(Long id) {
        //1.获取登录用户
        UserDTO userDTO = UserDTOHolder.getUserDTO();
        Long userId = userDTO.getId();
        //2.判断当前登录用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        Boolean liked = (score != null);
        if (BooleanUtil.isFalse(liked)) {
            //3.如果未点赞，可以点赞
            //3.1保存到Redis中
            Boolean addSuccess = stringRedisTemplate.opsForZSet().add(key, String.valueOf(userId), System.currentTimeMillis());
            //3.2Redis操作成功，数据库点赞数+1
            if (BooleanUtil.isTrue(addSuccess)) {
                blogMapper.plusBlogLiked(id);
            }
        } else {
            //4.如果已点赞，不能点赞
            //4.1Redis删除该用户
            Long cancelSuccess = stringRedisTemplate.opsForZSet().remove(key, String.valueOf(userId));
            //4.2Redis操作成功，数据库点赞数-1
            if (cancelSuccess != null && cancelSuccess != 0) {
                blogMapper.subBlogLiked(id);
            }
        }
        //5.返回结果
        return Result.ok();
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userMapper.queryUserById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogLikedById(Long id) {
        //1.查询top5的点赞用户 zrange key 0 4
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> userIds = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (userIds == null || userIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //2.解析出对应的用户id
        List<Long> userIdList = userIds.stream().map(Long::valueOf).collect(Collectors.toList());
        //3.根据用户id查询用户
        List<UserDTO> userDTOS = new ArrayList<>();
        userIdList.forEach(userId -> {
            userDTOS.add(userMapper.queryUserDTOById(userId));
        });
        //4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserDTOHolder.getUserDTO();
        blog.setUserId(user.getId());
        // 2.保存探店博文
        boolean save = save(blog);
        if (!save) {
            return Result.fail("保存失败");
        }
        // 3.发送给关注他的粉丝
        // 3.1获取关注他的粉丝
        List<Long> fansIds = followMapper.getFansById(blog.getUserId());
        // 3.2推送笔记id给所有粉丝
        String keyPrefix = RedisConstants.FEED_KEY;
        fansIds.forEach(fanId -> {
            stringRedisTemplate.opsForZSet().add(keyPrefix + fanId, blog.getId().toString(), System.currentTimeMillis());
        });

        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1.找到用户收件箱
        UserDTO userDTO = UserDTOHolder.getUserDTO();
        Long userId = userDTO.getId();
        String key = RedisConstants.FEED_KEY + userId;
        // WithScores方法能获得对应时间戳
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        //2.分页查询数据
        //2.1解析数据：ids，最小时间给max，最后一个值相同值offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        List<Long> scores = new ArrayList<>(typedTuples.size());
        typedTuples.forEach(tuple -> {
            ids.add(Long.valueOf(tuple.getValue()));
            scores.add(tuple.getScore().longValue());
        });
        max = scores.get(scores.size() - 1);
        offset = 1;
        for (int i = scores.size() - 1; i > 0 && scores.get(i) == scores.get(i - 1); i--) {
            offset++;
        }
        //3.返回
        List<Blog> blogs = new ArrayList<>();
        ids.forEach(id -> {
            blogs.add(blogMapper.queryBlogById(id));
        });
        //需要对每个博客进行查询相关用户，是否点赞的信息
        blogs.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(offset);
        scrollResult.setMinTime(max);
        return Result.ok(scrollResult);
    }
}
