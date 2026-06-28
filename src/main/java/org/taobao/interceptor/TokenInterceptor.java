package org.taobao.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.taobao.context.BaseContext;
import org.taobao.utils.JwtUtils;

@Component
public class TokenInterceptor implements HandlerInterceptor {
    //日志输出，记录拦截http请求的过程
    private static final Logger log = LoggerFactory.getLogger(TokenInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求头中的令牌（token），支持Authorization头和Bearer前缀
        String jwt = request.getHeader("token");
        
        // 如果token为空，尝试从Authorization头获取
        if(!StringUtils.hasLength(jwt)) {
            String authorization = request.getHeader("Authorization");
            // 检查authorization是否有值且以"Bearer "开头
            if(StringUtils.hasLength(authorization) && authorization.startsWith("Bearer ")) {
                jwt = authorization.substring(7);//去掉Bearer前缀并赋值给jwt
            }
        }

        //判断令牌是否存在，如果不存在，返回错误结果（未登录）。
        if(!StringUtils.hasLength(jwt)){ //jwt为空
            log.info("获取到jwt令牌为空, 返回错误结果");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }

        // 解析token，如果解析失败，返回错误结果（未登录）。
        try {
            // 解析JWT令牌
            Claims claims = JwtUtils.parseJWT(jwt);
            // 获取用户ID，添加空值检查
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                log.info("令牌中未包含用户ID, 返回错误结果");
                response.setStatus(HttpStatus.SC_UNAUTHORIZED);
                // 添加更详细的错误信息
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"令牌中未包含用户ID，请重新登录\",\"data\":null}");
                return false;
            }
            // 将用户ID转换为Long类型并存储到BaseContext中，处理不同类型的情况
            Long userId;
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else {
                userId = Long.valueOf(userIdObj.toString());
            }
            BaseContext.setCurrentId(userId);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("解析令牌失败, 返回错误结果");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }

        // 放行。
        log.info("令牌合法, 放行");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除当前线程的用户ID，避免内存泄漏
        BaseContext.removeCurrentId();
    }
}