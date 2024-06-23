package com.hmdp.utils;

import cn.hutool.jwt.JWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.PublicKey;
import java.util.Map;

public class JwtUtils {

    private static String singKey = "alfo";

    //首次登录时获取jwt令牌
    //这里不需要指定过期时间，在redis中控制
    public static String generateJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, singKey)    //指定签名算法 和 密匙
                .setClaims(claims)
                .compact();
    }

    public static Claims parseJwt(String jwt) {
        return Jwts.parser()
                .setSigningKey(singKey) //set sign key
                .parseClaimsJws(jwt)    //parse got jwt
                .getBody(); //make (String)jwt to (Map)Claims
    }
}
