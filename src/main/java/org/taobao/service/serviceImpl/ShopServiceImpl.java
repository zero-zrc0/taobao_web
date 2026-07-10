package org.taobao.service.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.taobao.constant.ShopStatusConstant;
import org.taobao.dto.ShopCreateDTO;
import org.taobao.dto.ShopQueryDTO;
import org.taobao.dto.ShopStatisticsDTO;
import org.taobao.dto.ShopUpdateDTO;
import org.taobao.exception.ShopNotFoundException;
import org.taobao.exception.ShopNameAlreadyExistsException;
import org.taobao.mapper.ShopMapper;
import org.taobao.pojo.Shop;
import org.taobao.service.ShopService;
import org.taobao.utils.OssUrlUtils;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.List;

/**
 * 店铺Service实现类
 */
@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public Shop getShopById(Integer shopId) {
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }
        return shop;
    }

    @Override
    public Shop getShopByMerchantId(Integer merchantId) {
        Shop shop = shopMapper.findByMerchantId(merchantId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }
        return shop;
    }

    @Override
    public void createShop(Integer merchantId, ShopCreateDTO shopCreateDTO) {
        // 检查店铺名称是否已存在
        Shop existingShop = shopMapper.findByShopName(shopCreateDTO.getShopName());
        if (existingShop != null) {
            throw new ShopNameAlreadyExistsException("店铺名称已存在");
        }

        // 检查商家是否已有店铺
        existingShop = shopMapper.findByMerchantId(merchantId);
        if (existingShop != null) {
            throw new RuntimeException("一个商家只能有一个店铺");
        }

        // 创建店铺对象
        Shop shop = new Shop();
        shop.setMerchantId(merchantId);
        shop.setShopName(shopCreateDTO.getShopName());
        shop.setShopDescription(shopCreateDTO.getShopDescription());

        // 处理店铺logo和横幅图片，去除OSS前缀并转换为JSON数组格式存储
        // 处理店铺logo和横幅图片，存储完整URL
        if (shopCreateDTO.getShopLogo() != null && !shopCreateDTO.getShopLogo().isEmpty()) {
            shop.setShopLogo(OssUrlUtils.extractObjectKey(shopCreateDTO.getShopLogo()));
        }

        if (shopCreateDTO.getShopBanner() != null && !shopCreateDTO.getShopBanner().isEmpty()) {
            shop.setShopBanner(OssUrlUtils.extractObjectKey(shopCreateDTO.getShopBanner()));
        }

        shop.setStatus("normal"); // 默认正常状态，创建即激活

        Date now = new Date();
        shop.setCreateTime(now);
        shop.setUpdateTime(now);

        // 插入数据库
        shopMapper.insert(shop);
    }

    @Override
    public void updateShop(Integer shopId, ShopUpdateDTO shopUpdateDTO) {
        // 检查店铺是否存在
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }

        // 如果修改了店铺名称，检查是否已存在
        if (shopUpdateDTO.getShopName() != null && !shopUpdateDTO.getShopName().equals(shop.getShopName())) {
            Shop existingShop = shopMapper.findByShopName(shopUpdateDTO.getShopName());
            if (existingShop != null) {
                throw new ShopNameAlreadyExistsException("店铺名称已存在");
            }
            shop.setShopName(shopUpdateDTO.getShopName());
        }

        // 更新其他字段
        if (shopUpdateDTO.getShopDescription() != null) {
            shop.setShopDescription(shopUpdateDTO.getShopDescription());
        }

        // 处理店铺logo和横幅图片，存储完整URL
        if (shopUpdateDTO.getShopLogo() != null) {
            if (shopUpdateDTO.getShopLogo().isEmpty()) {
                shop.setShopLogo(null);
            } else {
                shop.setShopLogo(OssUrlUtils.extractObjectKey(shopUpdateDTO.getShopLogo()));
            }
        }

        if (shopUpdateDTO.getShopBanner() != null) {
            if (shopUpdateDTO.getShopBanner().isEmpty()) {
                shop.setShopBanner(null);
            } else {
                shop.setShopBanner(OssUrlUtils.extractObjectKey(shopUpdateDTO.getShopBanner()));
            }
        }

        if (shopUpdateDTO.getStatus() != null) {
            // 验证status字段的有效性
            List<String> validStatuses = Arrays.asList(
                    ShopStatusConstant.NORMAL,
                    ShopStatusConstant.CLOSED,
                    ShopStatusConstant.AUDITING);

            if (!validStatuses.contains(shopUpdateDTO.getStatus())) {
                throw new IllegalArgumentException("无效的店铺状态值: " + shopUpdateDTO.getStatus());
            }

            shop.setStatus(shopUpdateDTO.getStatus());
        }

        // 设置更新时间
        shop.setUpdateTime(new Date());

        // 更新数据库
        shopMapper.update(shop);
    }

    @Override
    public void updateShopByMerchantId(Integer merchantId, ShopUpdateDTO shopUpdateDTO) {
        // 检查店铺是否存在
        Shop shop = shopMapper.findByMerchantId(merchantId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }

        // 如果修改了店铺名称，检查是否已存在
        if (shopUpdateDTO.getShopName() != null && !shopUpdateDTO.getShopName().equals(shop.getShopName())) {
            Shop existingShop = shopMapper.findByShopName(shopUpdateDTO.getShopName());
            if (existingShop != null) {
                throw new ShopNameAlreadyExistsException("店铺名称已存在");
            }
            shop.setShopName(shopUpdateDTO.getShopName());
        }

        // 更新其他字段
        if (shopUpdateDTO.getShopDescription() != null) {
            shop.setShopDescription(shopUpdateDTO.getShopDescription());
        }

        // 处理店铺logo和横幅图片，存储完整URL
        if (shopUpdateDTO.getShopLogo() != null) {
            if (shopUpdateDTO.getShopLogo().isEmpty()) {
                shop.setShopLogo(null);
            } else {
                // 存储完整URL
                shop.setShopLogo(shopUpdateDTO.getShopLogo());
            }
        }

        if (shopUpdateDTO.getShopBanner() != null) {
            if (shopUpdateDTO.getShopBanner().isEmpty()) {
                shop.setShopBanner(null);
            } else {
                // 存储完整URL
                shop.setShopBanner(shopUpdateDTO.getShopBanner());
            }
        }

        if (shopUpdateDTO.getStatus() != null) {
            // 验证status字段的有效性
            List<String> validStatuses = Arrays.asList(
                ShopStatusConstant.NORMAL,
                ShopStatusConstant.CLOSED,
                ShopStatusConstant.AUDITING
            );
            
            if (!validStatuses.contains(shopUpdateDTO.getStatus())) {
                throw new IllegalArgumentException("无效的店铺状态值: " + shopUpdateDTO.getStatus());
            }
            
            shop.setStatus(shopUpdateDTO.getStatus());
        }

        // 设置更新时间
        shop.setUpdateTime(new Date());

        // 更新数据库
        shopMapper.updateByMerchantId(shop);
    }

    @Override
    public List<Shop> getShopList(ShopQueryDTO shopQueryDTO) {
        return shopMapper.getShopList(shopQueryDTO);
    }

    @Override
    public Integer getShopCount(ShopQueryDTO shopQueryDTO) {
        return shopMapper.getShopCount(shopQueryDTO);
    }

    @Override
    public ShopStatisticsDTO getShopStatistics(Integer shopId) {
        // 检查店铺是否存在
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }
        return shopMapper.getShopStatistics(shopId);
    }

    @Override
    public void auditShop(Integer shopId, String status) {
        // 检查店铺是否存在
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }

        // 更新店铺状态
        shop.setStatus(status);
        shop.setUpdateTime(new Date());

        // 更新数据库
        shopMapper.update(shop);
    }

    @Override
    public void closeShop(Integer shopId) {
        // 检查店铺是否存在
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }

        // 更新店铺状态为关闭
        shop.setStatus("closed");
        shop.setUpdateTime(new Date());

        // 更新数据库
        shopMapper.update(shop);
    }

    @Override
    public void reopenShop(Integer shopId) {
        // 检查店铺是否存在
        Shop shop = shopMapper.findById(shopId);
        if (shop == null) {
            throw new ShopNotFoundException("店铺不存在");
        }

        // 更新店铺状态为正常
        shop.setStatus("normal");
        shop.setUpdateTime(new Date());

        // 更新数据库
        shopMapper.update(shop);
    }
}
