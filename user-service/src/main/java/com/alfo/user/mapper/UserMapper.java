package com.alfo.user.mapper;



import com.alfo.common.domain.dto.UserDTO;
import com.alfo.user.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from tb_user where phone = #{phone}")
    User getUser(String phone);

    @Insert("insert into tb_user(phone, password, nick_name, create_time, update_time) values (#{phone}, #{password}, #{nickName}, #{createTime}, #{updateTime})")
    void insert(String phone, String password, String nickName, LocalDateTime createTime, LocalDateTime updateTime);

    @Select("select id, nick_name, icon from tb_user where id <= 1000;")
    List<UserDTO> getUserDTOS();

    @Select("select * from tb_user where id = #{id}")
    User queryUserById(Long id);

    @Select("select id, nick_name, icon  from tb_user where  id = #{userId}")
    UserDTO queryUserDTOById(Long userId);
}
