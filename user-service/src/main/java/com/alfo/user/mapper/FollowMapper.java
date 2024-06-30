package com.alfo.user.mapper;



import com.alfo.user.domain.po.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface FollowMapper extends BaseMapper<Follow> {

    @Insert("insert into tb_follow(user_id, follow_user_id) values (#{userId}, #{followUserId})")
    void insertFollow(Follow follow);

    @Delete("delete from tb_follow where user_id = #{userId} and follow_user_id = #{followUserId}")
    void deleteFollowByIds(Long followUserId, Long userId);

    @Select("select count(*) from tb_follow where follow_user_id = #{followUserId} and user_id = #{userId}")
    Integer queryFollowByIds(Long followUserId, Long userId);

    @Select("select user_id from tb_follow where follow_user_id = #{followUserId}")
    List<Long> getFansById(Long followUserId);
}
