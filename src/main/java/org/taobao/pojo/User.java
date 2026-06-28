package org.taobao.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户表实体类
 */
@Data
@Builder//生成建造者模式，支持链式调用
@NoArgsConstructor//生成无参构造方法
@AllArgsConstructor//生成全参构造方法
public class User {
    private Long userId; // 用户唯一标识ID
    private String account; // 账号，用于登录
    private String password; // 加密后的密码
    private String userType; // 用户类型：operator, merchant, customer, visitor
    private String status; // 账户状态：active, inactive, locked
    private String username; // 用户名（网名）
    private String gender; // 性别：male, female, unknown
    private Date birthday; // 出生日期
    private String phone; // 手机号码
    private String email; // 邮箱地址
    private String avatarUrl; // 用户头像图片URL
    private Date createTime; // 创建时间
    private Date updateTime; // 修改时间
}