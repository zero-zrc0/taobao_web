package org.taobao.context;

/**
 * 线程上下文，用于存储当前用户信息‘
 * 使用 ThreadLocal 存储当前登录用户的 ID
 * 拦截器验证 JWT 后存入用户 ID
 * Controller/Service 层随时获取，无需层层传参
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     * 
     * @param id 用户ID
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 清除当前用户ID
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}