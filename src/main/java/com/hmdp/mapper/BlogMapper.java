package com.hmdp.mapper;

import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface BlogMapper extends BaseMapper<Blog> {

    @Select("select * from tb_blog where  id = #{id}")
    Blog queryBlogById(Long id);

    @Update("update tb_blog set liked = liked + 1 where id = #{id};")
    void plusBlogLiked(Long id);

    @Update("update tb_blog set liked = liked - 1 where id = #{id};")
    void subBlogLiked(Long id);
}
