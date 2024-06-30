package com.alfo.user.service;


import com.alfo.common.domain.dto.Result;
import com.alfo.user.domain.po.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryHotBlog(Integer current);

    Result queryBlogLikedById(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
