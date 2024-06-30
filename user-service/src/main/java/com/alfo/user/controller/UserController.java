package com.alfo.user.controller;




import com.alfo.common.domain.dto.Result;
import com.alfo.common.utils.UserDTOHolder;
import com.alfo.user.domain.dto.LoginFormDTO;
import com.alfo.user.domain.po.AccessTokenResponse;
import com.alfo.user.domain.po.UserInfo;
import com.alfo.user.service.IUserInfoService;
import com.alfo.user.service.IUserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        log.info("进行短信验证码生成：");
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // TODO 实现登录功能
       return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        // TODO 获取当前登录的用户并返回
        return  Result.ok(UserDTOHolder.getUserDTO());
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result getUser(@PathVariable("id") Long userId) {
        return userService.queryUserById(userId);
    }

    @PostMapping("/sign")
    public Result sign() {
        return userService.signToday();
    }

    @GetMapping("/sign/count")
    public Result continueSignCount() {
        return userService.continueSignCount();
    }


    //使用github登录
    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @GetMapping("/oauth/redirect")
    public Result handleRedirect(@RequestParam("code") String requestToken, Model model) {
        //使用RestTemplate来发送HTTP请求
        RestTemplate restTemplate = new RestTemplate();

        //获取Token的url
        String tokenUrl = "https://github.com/login/oauth/access_token" +
                "?client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code=" + requestToken;

        // 使用restTemplate向GitHub发送请求，获取Token
        AccessTokenResponse tokenResponse = restTemplate.postForObject(tokenUrl, null, AccessTokenResponse.class);

        // 从响应体中获取Token数据
        String accessToken = tokenResponse.getAccessToken();

        // 携带Token向GitHub发送请求
        String apiUrl = "https://api.github.com/user";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
        model.addAttribute("userData", response.getBody());
        return Result.ok(model);
    }
}
