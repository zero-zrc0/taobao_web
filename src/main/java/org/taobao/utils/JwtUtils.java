package org.taobao.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtils {

    private static String signKey = "SVRIRUlNQQ==";
    private static Long expire = 31536000000L; // 一年的毫秒数：365 * 24 * 3600 * 1000

    /**
     * 生成JWT令牌
     * @param claims 存储在令牌中的声明信息
     * @return JWT令牌字符串
     */
    public static String generateJwt(Map<String, Object> claims){
        String jwt = Jwts.builder()//创建JWT构建器
                .addClaims(claims)//添加用户信息到Payload
                .signWith(SignatureAlgorithm.HS256, signKey)//用加密算法和密钥签名防篡改
                .setExpiration(new Date(System.currentTimeMillis() + expire))//设置token有效期
                .compact();//将Header、Payload、Signature三部分组合成紧凑的JWT字符串
        return jwt;
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJWT(String jwt){
        Claims claims = Jwts.parser()//创建JWT解析器
                .setSigningKey(signKey)//配置解密密钥，验证token是否被篡改
                .parseClaimsJws(jwt)//解析JWT并验证签名是否有效
                .getBody();//提取Claims
        return claims;
    }
}