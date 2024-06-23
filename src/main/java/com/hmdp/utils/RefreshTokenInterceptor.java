package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的token
        String token = request.getHeader("authorization");
        //2.基于token获取redis中的用户
        //fillBeanWithMap参数：map，new Object()，boolean isIgnoreError
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        //3.判断用户是否存在
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(tokenKey);
        if (map.isEmpty()) {
            //4.不存在
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(stringRedisTemplate.opsForHash().entries(tokenKey),
                new UserDTO(), false);
        //5.存在，将UserDTO存到ThreadLocal中
        UserDTOHolder.saveUserDTO(userDTO);
        //6.刷新token
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //7.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserDTOHolder.removeUserDTO();
    }
}
