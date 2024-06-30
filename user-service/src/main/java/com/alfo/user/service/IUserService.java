package com.alfo.user.service;


import com.alfo.common.domain.dto.Result;

import com.alfo.user.domain.dto.LoginFormDTO;
import com.alfo.user.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;


import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result queryUserById(Long userId);

    Result signToday();

    Result continueSignCount();
}
