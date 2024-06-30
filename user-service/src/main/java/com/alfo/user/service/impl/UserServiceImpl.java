package com.alfo.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;


import com.alfo.common.domain.dto.Result;
import com.alfo.common.domain.dto.UserDTO;
import com.alfo.common.utils.UserDTOHolder;
import com.alfo.common.utils.constants.UserConstants;
import com.alfo.common.utils.redis.RedisConstants;
import com.alfo.common.utils.redis.RegexUtils;
import com.alfo.user.domain.dto.LoginFormDTO;
import com.alfo.user.domain.po.User;
import com.alfo.user.mapper.UserMapper;
import com.alfo.user.service.IUserService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.获取手机号，并校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合返回
            return Result.fail("手机号格式错误!");
        }
        //3.如果符合要求，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到redis     //set key value value ex
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        log.info("验证码发送成功，验证码为：{}", code);
        //6.返回信息
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号，不符合返回（两次是独立的请求，也要校验）
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号错误！");
        }
        //2.校验验证码，不一致返回(比较loginForm提交的code和session中的code)
        if (session.getAttribute("code") != null && !loginForm.getCode().equals((String)session.getAttribute("code"))) {
            return Result.fail("验证码不一致");
        }
        //3.根据手机号查询用户
        User user = userMapper.getUser(phone);
        //4.用户不存在，创建新用户，保存到数据库
        //这里修改默认名字为user_xxxxxx
        if (user == null) {
            userMapper.insert(UserConstants.USER_NAME, phone, loginForm.getPassword(), LocalDateTime.now(), LocalDateTime.now());
        }
        //5.如果用户存在,保存用户到redis中
        //格式：key-value : token:XXXXXXXXX - {name:lisi.....}(hash)
        user = userMapper.getUser(phone);
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO,
                new HashMap<>(), CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userDTOMap);
        //hash需要手动定义时间
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6.返回
        return Result.ok(token);
    }

    @Override
    public Result queryUserById(Long userId) {
        return Result.ok(userMapper.queryUserDTOById(userId));
    }

    @Override
    public Result signToday() {
        //1. 获取签到需要的信息：用户信息、当天年月(yyyyMM)
        Long userId = UserDTOHolder.getUserDTO().getId();
        LocalDate todayDate = LocalDate.now();
        String yearMonth = todayDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
        //2. 获取offset（今天第几天）
        int dayOfMonth = todayDate.getDayOfMonth();
        //3.写到redis：setbit key offset 0/1
        String key = RedisConstants.USER_SIGN_KEY + userId + ":" + yearMonth;
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result continueSignCount() {
        //1.bitfield key get uoffset 0
        //1.1获取要在Redis中查询的key（sign:1010:202405）
        String key = RedisConstants.USER_SIGN_KEY +
                UserDTOHolder.getUserDTO().getId() + ":" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        //1.2获取offset
        int offset = LocalDateTime.now().getDayOfMonth();
        //2.查询Redis获取十进制并转为二进制字符串
        //为什么会是集合？——因为bitField可以对多个数据进行操作，这里我们知道只需要第一个数据
        List<Long> results = stringRedisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(offset)).valueAt(0)
        );
        if (results == null || results.isEmpty()) {
            return Result.ok(0);
        }
        Long num = results.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        //3.与1进行与运算，结果为0结束，获得结果
        int resultDays = 0;
        while (true) {
            if ((num & 1) == 0) {
                break;
            }
            resultDays++;
            num = num>>1;
        }
        //4.获取结果
        return Result.ok(resultDays);
    }
}
