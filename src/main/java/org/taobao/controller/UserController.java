package org.taobao.controller;

import org.taobao.context.BaseContext;
import org.taobao.dto.UserLoginDTO;
import org.taobao.dto.UserProfileUpdateDTO;
import org.taobao.dto.UserRegisterDTO;
import org.taobao.exception.AccountLockedException;
import org.taobao.exception.AccountNotFoundException;
import org.taobao.exception.PasswordErrorException;
import org.taobao.pojo.Result;
import org.taobao.pojo.User;
import org.taobao.service.UserService;
import org.taobao.service.OrderService;
import org.taobao.utils.AliyunOSSOperator;
import org.taobao.utils.JwtUtils;
import org.taobao.vo.UserLoginVO;
import org.taobao.vo.UserProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        try {
            // 用户登录
            User user = userService.login(userLoginDTO);

            // 登录成功，生成JWT令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("account", user.getAccount());
            String token = JwtUtils.generateJwt(claims);

            // 构建返回结果
            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .account(user.getAccount())
                    .username(user.getUsername() != null && !user.getUsername().isEmpty() ? user.getUsername()
                            : user.getAccount())
                    .userType(user.getUserType())
                    .token(token)
                    .build();

            return Result.success(userLoginVO);
        } catch (AccountNotFoundException | PasswordErrorException | AccountLockedException e) {
            // 捕获登录相关异常，返回错误信息
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 捕获其他异常，返回通用错误信息
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        try {
            // 验证必填字段
            if (userRegisterDTO.getAccount() == null || userRegisterDTO.getAccount().trim().isEmpty()) {
                return Result.error("账号不能为空");
            }
            if (userRegisterDTO.getPassword() == null || userRegisterDTO.getPassword().trim().isEmpty()) {
                return Result.error("密码不能为空");
            }
            if (userRegisterDTO.getUserType() == null || userRegisterDTO.getUserType().trim().isEmpty()) {
                return Result.error("用户类型不能为空");
            }

            // 验证账号长度
            String account = userRegisterDTO.getAccount();
            if (account.length() < 6 || account.length() > 20) {
                return Result.error("账号长度必须在6-20个字符之间");
            }

            // 验证密码长度
            String password = userRegisterDTO.getPassword();
            if (password.length() < 6 || password.length() > 20) {
                return Result.error("密码长度必须在6-20个字符之间");
            }

            // 验证用户类型是否合法
            String userType = userRegisterDTO.getUserType();
            if (!"operator".equals(userType) && !"merchant".equals(userType) &&
                    !"customer".equals(userType) && !"visitor".equals(userType)) {
                return Result.error("用户类型必须为operator、merchant、customer或visitor");
            }

            // 调用服务层进行注册
            userService.register(userRegisterDTO);
            return Result.success("用户注册成功");
        } catch (Exception e) {
            return Result.error("用户注册失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户个人详情（包含头像URL）
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getUserProfile() {
        try {
            // 从BaseContext中获取当前用户ID
            Long userId = BaseContext.getCurrentId();
            // 调用服务层获取用户详情
            User user = userService.getUserProfile(userId);

            // 构建用户详情VO，不包含密码等敏感信息
            UserProfileVO userProfileVO = new UserProfileVO();
            userProfileVO.setUserId(user.getUserId());
            userProfileVO.setAccount(user.getAccount());
            userProfileVO.setUserType(user.getUserType());
            userProfileVO.setStatus(user.getStatus());
            userProfileVO.setUsername(user.getUsername());
            userProfileVO.setGender(user.getGender());
            userProfileVO.setBirthday(user.getBirthday());
            userProfileVO.setPhone(user.getPhone());
            userProfileVO.setEmail(user.getEmail());

            // 从完整URL中提取中间的数据和后缀，格式为：yyyy/MM/UUID.xxx
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                // 完整URL格式：https://bucket-name.oss-region.aliyuncs.com/yyyy/MM/UUID.xxx
                // 找到第4个斜杠的位置，即域名后的第一个斜杠
                int firstSlashIndex = user.getAvatarUrl().indexOf("/");
                firstSlashIndex = user.getAvatarUrl().indexOf("/", firstSlashIndex + 1);
                firstSlashIndex = user.getAvatarUrl().indexOf("/", firstSlashIndex + 1);

                if (firstSlashIndex != -1) {
                    // 直接生成签名URL
                    String objectName = user.getAvatarUrl().substring(firstSlashIndex + 1);
                    userProfileVO.setAvatarUrl(objectName);

                } else {
                    userProfileVO.setAvatarUrl(user.getAvatarUrl());
                }
            }

            userProfileVO.setCreateTime(user.getCreateTime());
            userProfileVO.setUpdateTime(user.getUpdateTime());

            // 获取订单状态统计
            Map<String, Long> orderStats = orderService.getOrderStatusStatistics(user.getUserId().intValue());
            // 设置订单状态统计到VO中
            userProfileVO.setPendingOrderCount(orderStats.getOrDefault("pending", 0L));
            userProfileVO.setPaidOrderCount(orderStats.getOrDefault("paid", 0L));
            userProfileVO.setShippedOrderCount(orderStats.getOrDefault("shipped", 0L));
            userProfileVO.setCompletedOrderCount(orderStats.getOrDefault("completed", 0L));
            userProfileVO.setCancelledOrderCount(orderStats.getOrDefault("cancelled", 0L));

            return Result.success(userProfileVO);
        } catch (Exception e) {
            return Result.error("获取用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 修改用户个人详情（不包含头像）
     */
    @PutMapping("/profile/update")
    public Result<String> updateUserProfile(@RequestBody UserProfileUpdateDTO userProfileUpdateDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Long userId = BaseContext.getCurrentId();

            // 调用服务层更新用户详情
            userService.updateUserProfile(userId, userProfileUpdateDTO);

            // 返回成功提示
            return Result.success("修改用户详情成功");
        } catch (Exception e) {
            return Result.error("修改用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 修改用户头像
     */
    @PostMapping("/profile/avatar/upload")
    public Result<String> updateAvatar(@RequestPart("avatar") MultipartFile avatar) {
        try {
            // 从BaseContext中获取当前用户ID
            Long userId = BaseContext.getCurrentId();

            // 生成唯一文件名
            String originalFilename = avatar.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;
            // 上传文件到OSS
            String fullAvatarUrl = aliyunOSSOperator.upload(avatar.getBytes(), uniqueFileName);

            // 提取相对路径（去掉OSS域名，只保留yyyy/MM/UUID.xxx部分）
            // 完整URL格式：https://bucket-name.oss-region.aliyuncs.com/yyyy/MM/UUID.xxx
            int firstSlashIndex = fullAvatarUrl.indexOf("/");
            firstSlashIndex = fullAvatarUrl.indexOf("/", firstSlashIndex + 1);
            firstSlashIndex = fullAvatarUrl.indexOf("/", firstSlashIndex + 1);
            String objectName = fullAvatarUrl.substring(firstSlashIndex + 1);

            // 创建DTO对象，设置相对路径到数据库（避免签名URL过长）
            UserProfileUpdateDTO userProfileUpdateDTO = new UserProfileUpdateDTO();
            userProfileUpdateDTO.setAvatarUrl(objectName);
            // 调用服务层更新用户详情
            userService.updateUserProfile(userId, userProfileUpdateDTO);

            // 返回成功提示和关键字符
            return Result.success(objectName);
        } catch (Exception e) {
            return Result.error("修改头像失败：" + e.getMessage());
        }
    }

}