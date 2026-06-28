package org.taobao.controller;

import org.taobao.dto.OrderQueryDTO;
import org.taobao.dto.ShopCreateDTO;
import org.taobao.dto.UserProfileUpdateDTO;
import org.taobao.dto.UserQueryDTO;
import org.taobao.pojo.Orders;
import org.taobao.pojo.Result;
import org.taobao.pojo.User;
import org.taobao.service.AdminService;
import org.taobao.service.ShopService;
import org.taobao.service.UserService;
import org.taobao.utils.AliyunOSSOperator;
import org.taobao.vo.AdminDashboardVO;
import org.taobao.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 管理员首页统计接口
     * 包括今日新增用户数、今日交易额、今日新增订单数、今日完成订单数
     * 执行流程：
     * 1.Service 层查询数据库统计信息
     * 2.封装到 AdminDashboardVO 对象
     * 3.成功返回 JSON，失败返回错误提示
     */
    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> getDashboardData() {
        try {
            AdminDashboardVO dashboardData = adminService.getDashboardData();
            return Result.success(dashboardData);
        } catch (Exception e) {
            return Result.error("获取首页统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户列表（分页）
     * 路径：/admin/user/list
     */
    @GetMapping("/user/list")
    public Result<PageResult<User>> getUserList(UserQueryDTO userQueryDTO) {
        try {
            // 获取用户列表
            List<User> userList = userService.getUserList(userQueryDTO);
            // 获取用户总数
            Integer total = userService.getUserCount(userQueryDTO);
            // 构建分页结果，封装分页对象
            PageResult<User> pageResult = PageResult.build(userList, total.longValue(), userQueryDTO.getPageNum(),
                    userQueryDTO.getPageSize());
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取普通用户列表（分页）
     * 路径：/admin/user/customer/list
     */
    @GetMapping("/user/customer/list")
    public Result<PageResult<User>> getCustomerUserList(UserQueryDTO userQueryDTO) {
        try {
            // 设置用户类型为普通用户
            userQueryDTO.setUserType("customer");
            // 获取用户列表
            List<User> userList = userService.getUserList(userQueryDTO);
            // 获取用户总数
            Integer total = userService.getUserCount(userQueryDTO);
            // 构建分页结果
            PageResult<User> pageResult = PageResult.build(userList, total.longValue(), userQueryDTO.getPageNum(),
                    userQueryDTO.getPageSize());
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取普通用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取商家列表（分页）
     * 路径：/admin/user/merchant/list
     */
    @GetMapping("/user/merchant/list")
    public Result<PageResult<User>> getMerchantUserList(UserQueryDTO userQueryDTO) {
        try {
            // 设置用户类型为商家
            userQueryDTO.setUserType("merchant");
            // 获取用户列表
            List<User> userList = userService.getUserList(userQueryDTO);
            // 获取用户总数
            Integer total = userService.getUserCount(userQueryDTO);
            // 构建分页结果
            PageResult<User> pageResult = PageResult.build(userList, total.longValue(), userQueryDTO.getPageNum(),
                    userQueryDTO.getPageSize());
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取商家列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审核商家列表（分页）
     * 路径：/admin/user/merchant/pending/list
     */
    @GetMapping("/user/merchant/pending/list")
    public Result<PageResult<User>> getPendingMerchantUserList(UserQueryDTO userQueryDTO) {
        try {
            // 设置用户类型为商家
            userQueryDTO.setUserType("merchant");
            // 设置状态为锁定（待审核）
            userQueryDTO.setStatus("locked");
            // 获取用户列表
            List<User> userList = userService.getUserList(userQueryDTO);
            // 获取用户总数
            Integer total = userService.getUserCount(userQueryDTO);
            // 构建分页结果
            PageResult<User> pageResult = PageResult.build(userList, total.longValue(), userQueryDTO.getPageNum(),
                    userQueryDTO.getPageSize());
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取待审核商家列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     * 路径：/admin/user?id=14
     */
    @GetMapping("/user")
    public Result<User> getUserById(@RequestParam Long id) {
        try {
            User user = userService.getUserById(id);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     * 路径：/admin/user/status?id=14&status=active
     */
    @PutMapping("/user/status")
    public Result<String> updateUserStatus(@RequestParam Long id, @RequestParam String status) {
        try {
            userService.updateUserStatus(id, status);
            return Result.success("用户状态更新成功");
        } catch (Exception e) {
            return Result.error("用户状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 审核商家
     * 路径：/admin/user/merchant/audit?id=14&status=active
     */
    @PutMapping("/user/merchant/audit")
    public Result<String> auditMerchant(@RequestParam Long id, @RequestParam String status) {
        try {
            userService.updateUserStatus(id, status);

            // 如果审核通过，自动创建店铺
            if ("active".equals(status)) {
                // 创建默认店铺，使用商家用户名作为店铺名称
                User user = userService.getUserById(id);
                ShopCreateDTO shopCreateDTO = new ShopCreateDTO();
                // 使用商家用户名作为店铺名称，确保唯一
                shopCreateDTO.setShopName(user.getUsername() + "的店铺");
                shopCreateDTO.setShopDescription(user.getUsername() + "的官方店铺");
                // 自动创建店铺，与商家ID绑定
                shopService.createShop(user.getUserId().intValue(), shopCreateDTO);
            }

            return Result.success("商家审核成功");
        } catch (Exception e) {
            return Result.error("商家审核失败：" + e.getMessage());
        }
    }

    /**
     * 修改用户信息（不包含头像）
     * 路径：/admin/user/update?id=14
     */
    @PutMapping("/user/update")
    public Result<String> updateUser(@RequestParam Long id, @RequestBody UserProfileUpdateDTO userProfileUpdateDTO) {
        try {
            // 调用服务层更新用户详情（不包含头像）
            userService.updateUserProfile(id, userProfileUpdateDTO);
            return Result.success("用户信息修改成功");
        } catch (Exception e) {
            return Result.error("用户信息修改失败：" + e.getMessage());
        }
    }

    /**
     * 修改用户头像
     * 路径：/admin/user/avatar?id=14
     */
    @PostMapping("/user/avatar")
    public Result<String> updateUserAvatar(@RequestParam Long id, @RequestPart("avatar") MultipartFile avatar) {
        try {
            // 生成唯一文件名
            String originalFilename = avatar.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;
            // 上传文件到OSS
            String fullAvatarUrl = aliyunOSSOperator.upload(avatar.getBytes(), uniqueFileName);

            // 提取相对路径（去掉前缀，只保留yyyy/MM/xxx.jpg部分）
            // 完整URL格式：https://bucket-name.oss-region.aliyuncs.com/yyyy/MM/UUID.xxx
            // 找到第4个斜杠的位置，即域名后的第一个斜杠
            int firstSlashIndex = fullAvatarUrl.indexOf("/");
            firstSlashIndex = fullAvatarUrl.indexOf("/", firstSlashIndex + 1);
            firstSlashIndex = fullAvatarUrl.indexOf("/", firstSlashIndex + 1);

            // 提取从第4个斜杠开始的所有内容，即 yyyy/MM/UUID.xxx
            String objectName = fullAvatarUrl.substring(firstSlashIndex + 1);

            // 创建DTO对象，设置相对路径到数据库（避免签名URL过长）
            UserProfileUpdateDTO userProfileUpdateDTO = new UserProfileUpdateDTO();
            userProfileUpdateDTO.setAvatarUrl(objectName);

            // 调用服务层更新用户头像
            userService.updateUserProfile(id, userProfileUpdateDTO);

            // 返回成功提示和相对路径
            return Result.success(objectName);
        } catch (Exception e) {
            return Result.error("用户头像修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除用户
     * 路径：/admin/user/delete?id=14
     */
    @DeleteMapping("/user/delete")
    public Result<String> deleteUser(@RequestParam Long id) {
        try {
            // 这里可以调用现有的updateUserStatus方法，将用户状态设置为inactive
            // 实际删除操作可以根据业务需求实现，这里采用软删除
            userService.updateUserStatus(id, "inactive");
            return Result.success("用户删除成功");
        } catch (Exception e) {
            return Result.error("用户删除失败：" + e.getMessage());
        }
    }

    /**
     * 管理员获取订单列表（分页）
     * 路径：/admin/order/list
     */
    @GetMapping("/order/list")
    public Result<PageResult<Orders>> adminGetOrderList(OrderQueryDTO orderQueryDTO) {
        try {
            PageResult<Orders> pageResult = adminService.adminGetOrderList(orderQueryDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 管理员获取订单详情
     * 路径：/admin/order/detail?id=1
     */
    @GetMapping("/order/detail")
    public Result<Orders> adminGetOrderDetail(@RequestParam Integer id) {
        try {
            Orders order = adminService.adminGetOrderDetail(id);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("获取订单详情失败：" + e.getMessage());
        }
    }

    /**
     * 管理员取消订单
     * 路径：/admin/order/cancel?id=1&status=cancelled
     */
    @PutMapping("/order/cancel")
    public Result<String> adminCancelOrder(@RequestParam Integer id, @RequestParam String status) {
        try {
            adminService.adminCancelOrder(id, status);
            return Result.success("订单取消成功");
        } catch (Exception e) {
            return Result.error("订单取消失败：" + e.getMessage());
        }
    }
}