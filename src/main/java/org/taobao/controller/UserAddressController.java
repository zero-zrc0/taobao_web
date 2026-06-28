package org.taobao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.taobao.dto.UserAddressAddDTO;
import org.taobao.dto.UserAddressUpdateDTO;
import org.taobao.pojo.Result;
import org.taobao.pojo.UserAddress;
import org.taobao.service.UserAddressService;

import java.util.List;

/**
 * 用户地址控制器
 */
@RestController
@RequestMapping("/address")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 添加地址
     * 
     * @param userAddressAddDTO 地址添加DTO
     * @return 添加结果
     */
    @PostMapping("/add")
    public Result<String> addAddress(@RequestBody UserAddressAddDTO userAddressAddDTO) {
        try {
            userAddressService.addAddress(userAddressAddDTO);
            return Result.success("添加地址成功");
        } catch (Exception e) {
            return Result.error("添加地址失败：" + e.getMessage());
        }
    }

    /**
     * 更新地址
     * 
     * @param userAddressUpdateDTO 地址更新DTO
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<String> updateAddress(@RequestBody UserAddressUpdateDTO userAddressUpdateDTO) {
        try {
            userAddressService.updateAddress(userAddressUpdateDTO);
            return Result.success("更新地址成功");
        } catch (Exception e) {
            return Result.error("更新地址失败：" + e.getMessage());
        }
    }

    /**
     * 删除地址
     * 
     * @param addressId 地址ID
     * @return 删除结果
     */
    @DeleteMapping("/{addressId}")
    public Result<String> deleteAddress(@PathVariable Integer addressId) {
        try {
            userAddressService.deleteAddress(addressId);
            return Result.success("删除地址成功");
        } catch (Exception e) {
            return Result.error("删除地址失败：" + e.getMessage());
        }
    }

    /**
     * 设置默认地址
     * 
     * @param addressId 地址ID
     * @return 设置结果
     */
    @PutMapping("/set-default/{addressId}")
    //将路径中的addressId提取出来赋值给方法的参数
    public Result<String> setDefaultAddress(@PathVariable Integer addressId) {
        try {
            userAddressService.setDefaultAddress(addressId);
            return Result.success("设置默认地址成功");
        } catch (Exception e) {
            return Result.error("设置默认地址失败：" + e.getMessage());
        }
    }

    /**
     * 获取地址列表
     * 
     * @return 地址列表
     */
    @GetMapping("/list")
    public Result<List<UserAddress>> getAddressList() {
        try {
            List<UserAddress> addressList = userAddressService.getAddressList();
            return Result.success(addressList);
        } catch (Exception e) {
            return Result.error("获取地址列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取默认地址
     * 
     * @return 默认地址
     */
    @GetMapping("/default")
    public Result<UserAddress> getDefaultAddress() {
        try {
            UserAddress defaultAddress = userAddressService.getDefaultAddress();
            return Result.success(defaultAddress);
        } catch (Exception e) {
            return Result.error("获取默认地址失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取地址
     * 
     * @param addressId 地址ID
     * @return 地址信息
     */
    @GetMapping("/{addressId}")
    public Result<UserAddress> getAddressById(@PathVariable Integer addressId) {
        try {
            UserAddress address = userAddressService.getAddressById(addressId);
            return Result.success(address);
        } catch (Exception e) {
            return Result.error("获取地址失败：" + e.getMessage());
        }
    }
}
