package org.taobao.vo;

import lombok.Data;

import java.util.Date;

/**
 * 用户详情返回VO
 */
@Data//由Lombok提供，编译时自动创建getter和setter方法
public class UserProfileVO {
    private Long userId;
    private String account;
    private String userType;
    private String status;
    private String username;
    private String gender;
    private Date birthday;
    private String phone;
    private String email;
    private String avatarUrl;
    private Date createTime;
    private Date updateTime;
    
    // 订单状态统计
    private Long pendingOrderCount; // 待付款订单数
    private Long paidOrderCount; // 待发货订单数
    private Long shippedOrderCount; // 待收货订单数
    private Long completedOrderCount; // 待评价订单数
    private Long cancelledOrderCount; // 退换/售后订单数
}