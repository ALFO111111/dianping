package com.alfo.common.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String phone;
    private String password;
    private String nickName;
    private String icon = "";
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
