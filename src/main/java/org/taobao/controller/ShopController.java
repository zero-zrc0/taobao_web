package org.taobao.controller;

import org.taobao.context.BaseContext;
import org.taobao.dto.OrderQueryDTO;
import org.taobao.dto.ProductCreateDTO;
import org.taobao.dto.ProductQueryDTO;
import org.taobao.dto.ProductUpdateDTO;
import org.taobao.dto.ShopCreateDTO;
import org.taobao.dto.ShopQueryDTO;
import org.taobao.dto.ShopStatisticsDTO;
import org.taobao.dto.ShopUpdateDTO;
import org.taobao.dto.ShopDTO;
import org.taobao.utils.AliyunOSSOperator;
import org.taobao.vo.PageResult;
import org.taobao.exception.ShopNotFoundException;
import org.taobao.pojo.Orders;
import org.taobao.pojo.Product;
import org.taobao.pojo.Result;
import org.taobao.pojo.Shop;
import org.taobao.service.OrderService;
import org.taobao.service.ProductService;
import org.taobao.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 店铺Controller
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 根据店铺ID获取店铺信息
     * 
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    @GetMapping("/{shopId}")
    public Result<ShopDTO> getShopById(@PathVariable Integer shopId) {
        try {
            Shop shop = shopService.getShopById(shopId);
            ShopDTO shopDTO = ShopDTO.fromShop(shop);
            return Result.success(shopDTO);
        } catch (Exception e) {
            return Result.error("获取店铺信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前商家的店铺信息
     * 
     * @return 店铺信息，如果不存在返回null
     */
    @GetMapping("/my")
    public Result<ShopDTO> getMyShop() {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();
            Shop shop = shopService.getShopByMerchantId(merchantId);
            ShopDTO shopDTO = ShopDTO.fromShop(shop);
            return Result.success(shopDTO);
        } catch (ShopNotFoundException e) {
            // 店铺不存在时返回null，而不是500错误
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("获取店铺信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建店铺
     *
     * @param shopCreateDTO 店铺创建信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<String> createShop(@RequestBody ShopCreateDTO shopCreateDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();
            shopService.createShop(merchantId, shopCreateDTO);
            return Result.success("店铺创建成功，正在审核中");
        } catch (Exception e) {
            return Result.error("店铺创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新店铺信息
     * 
     * @param shopUpdateDTO 店铺更新信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<String> updateShop(@RequestBody ShopUpdateDTO shopUpdateDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();
            shopService.updateShopByMerchantId(merchantId, shopUpdateDTO);
            return Result.success("店铺信息更新成功");
        } catch (IllegalArgumentException e) {
            return Result.error("店铺信息更新失败：" + e.getMessage());
        } catch (Exception e) {
            return Result.error("店铺信息更新失败：" + e.getMessage());
        }
    }

    /**
     * 获取店铺列表
     *
     * @param shopQueryDTO 查询条件
     * @return 店铺分页结果
     */
    @GetMapping("/list")
    public Result<PageResult<ShopDTO>> getShopList(ShopQueryDTO shopQueryDTO) {
        try {
            List<Shop> shopList = shopService.getShopList(shopQueryDTO);
            // 获取店铺总数
            Integer total = shopService.getShopCount(shopQueryDTO);
            // 转换为ShopDTO列表
            List<ShopDTO> shopDTOList = new java.util.ArrayList<>();
            for (Shop shop : shopList) {
                shopDTOList.add(ShopDTO.fromShop(shop));
            }
            // 构建分页结果
            PageResult<ShopDTO> pageResult = PageResult.build(shopDTOList, total.longValue(), shopQueryDTO.getPageNum(),
                    shopQueryDTO.getPageSize());
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("获取店铺列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取店铺统计信息
     *
     * @param shopId 店铺ID
     * @return 店铺统计信息
     */
    @GetMapping("/statistics/{shopId}")
    public Result<ShopStatisticsDTO> getShopStatistics(@PathVariable Integer shopId) {
        try {
            ShopStatisticsDTO statistics = shopService.getShopStatistics(shopId);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error("获取店铺统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前商家店铺的统计信息
     *
     * @return 店铺统计信息，如果店铺不存在返回null
     */
    @GetMapping("/statistics/my")
    public Result<ShopStatisticsDTO> getMyShopStatistics() {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();
            // 获取当前商家的店铺
            Shop shop = shopService.getShopByMerchantId(merchantId);
            // 获取店铺统计信息
            ShopStatisticsDTO statistics = shopService.getShopStatistics(shop.getShopId());
            return Result.success(statistics);
        } catch (ShopNotFoundException e) {
            // 店铺不存在时返回null，而不是500错误
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("获取店铺统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 审核店铺
     *
     * @param shopId 店铺ID
     * @param status 审核状态：normal-通过，closed-拒绝
     * @return 审核结果
     */
    @PutMapping("/audit/{shopId}/{status}")
    public Result<String> auditShop(@PathVariable Integer shopId, @PathVariable String status) {
        try {
            shopService.auditShop(shopId, status);
            return Result.success("店铺审核成功");
        } catch (Exception e) {
            return Result.error("店铺审核失败：" + e.getMessage());
        }
    }

    /**
     * 关闭店铺
     * 
     * @param shopId 店铺ID
     * @return 关闭结果
     */
    @PutMapping("/close/{shopId}")
    public Result<String> closeShop(@PathVariable Integer shopId) {
        try {
            shopService.closeShop(shopId);
            return Result.success("店铺关闭成功");
        } catch (Exception e) {
            return Result.error("店铺关闭失败：" + e.getMessage());
        }
    }

    /**
     * 重新开店
     *
     * @param shopId 店铺ID
     * @return 重新开店结果
     */
    @PutMapping("/reopen/{shopId}")
    public Result<String> reopenShop(@PathVariable Integer shopId) {
        try {
            shopService.reopenShop(shopId);
            return Result.success("店铺重新开店成功");
        } catch (Exception e) {
            return Result.error("店铺重新开店失败：" + e.getMessage());
        }
    }

    /**
     * 获取商家商品列表
     * 
     * @param productQueryDTO 查询条件
     * @return 商品列表
     */
    @GetMapping("/product/list")
    public Result<List<Product>> getMerchantProductList(ProductQueryDTO productQueryDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();

            // 获取当前商家的店铺
            Shop shop = shopService.getShopByMerchantId(merchantId);
            if (shop == null) {
                return Result.success(Collections.emptyList());
            }

            // 设置shopId到查询条件中
            productQueryDTO.setShopId(shop.getShopId());

            // 调用服务层获取商品列表
            List<Product> productList = productService.getShopProductList(shop.getShopId(), productQueryDTO);
            return Result.success(productList);
        } catch (ShopNotFoundException e) {
            // 店铺不存在时返回空列表，而不是500错误
            return Result.success(Collections.emptyList());
        } catch (Exception e) {
            return Result.error("获取商家商品列表失败：" + e.getMessage());
        }
    }

    /**
     * 商家添加商品
     * 
     * @param productCreateDTO 商品创建信息
     * @return 创建结果
     */
    @PostMapping("/product/add")
    public Result<String> addProduct(@RequestBody ProductCreateDTO productCreateDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();

            // 获取当前商家的店铺
            Shop shop = shopService.getShopByMerchantId(merchantId);
            if (shop == null) {
                return Result.error("店铺不存在，无法添加商品");
            }

            // 设置shopId到商品创建信息中
            productCreateDTO.setShopId(shop.getShopId());

            // 调用服务层添加商品
            productService.createProduct(productCreateDTO);
            return Result.success("添加商品成功");
        } catch (Exception e) {
            return Result.error("添加商品失败：" + e.getMessage());
        }
    }

    /**
     * 商家更新商品
     * 
     * @param productId        商品ID
     * @param productUpdateDTO 商品更新信息
     * @return 更新结果
     */
    @PutMapping("/product/update/{productId}")
    public Result<String> updateProduct(@PathVariable Integer productId,
            @RequestBody ProductUpdateDTO productUpdateDTO) {
        try {
            // 调用服务层更新商品
            productService.updateProduct(productId, productUpdateDTO);
            return Result.success("更新商品成功");
        } catch (Exception e) {
            return Result.error("更新商品失败：" + e.getMessage());
        }
    }

    /**
     * 商家删除商品
     * 
     * @param productId 商品ID
     * @return 删除结果
     */
    @DeleteMapping("/product/{productId}")
    public Result<String> deleteProduct(@PathVariable Integer productId) {
        try {
            // 调用服务层删除商品
            productService.deleteProduct(productId);
            return Result.success("删除商品成功");
        } catch (Exception e) {
            return Result.error("删除商品失败：" + e.getMessage());
        }
    }

    /**
     * 商家添加SKU
     * 
     * @param sku 商品SKU信息
     * @return 添加结果
     */
    @PostMapping("/product/sku/add")
    public Result<String> addSku(@RequestBody org.taobao.pojo.ProductSku sku) {
        try {
            // 调用服务层添加SKU
            productService.addSku(sku);
            return Result.success("添加SKU成功");
        } catch (Exception e) {
            return Result.error("添加SKU失败：" + e.getMessage());
        }
    }

    /**
     * 商家更新SKU
     * 
     * @param skuId SKU ID
     * @param sku   SKU信息
     * @return 更新结果
     */
    @PutMapping("/product/sku/update/{skuId}")
    public Result<String> updateSku(@PathVariable Integer skuId, @RequestBody org.taobao.pojo.ProductSku sku) {
        try {
            // 调用服务层更新SKU
            productService.updateSku(skuId, sku);
            return Result.success("更新SKU成功");
        } catch (Exception e) {
            return Result.error("更新SKU失败：" + e.getMessage());
        }
    }

    /**
     * 商家删除SKU
     * 
     * @param skuId SKU ID
     * @return 删除结果
     */
    @DeleteMapping("/product/sku/{skuId}")
    public Result<String> deleteSku(@PathVariable Integer skuId) {
        try {
            // 调用服务层删除SKU
            productService.deleteSku(skuId);
            return Result.success("删除SKU成功");
        } catch (Exception e) {
            return Result.error("删除SKU失败：" + e.getMessage());
        }
    }

    /**
     * 商家获取订单列表
     * 
     * @param orderQueryDTO 查询条件
     * @return 订单列表
     */
    @GetMapping("/order/list")
    public Result<List<Orders>> getShopOrderList(@ModelAttribute OrderQueryDTO orderQueryDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();

            // 获取当前商家的店铺
            Shop shop = shopService.getShopByMerchantId(merchantId);
            if (shop == null) {
                return Result.success(Collections.emptyList());
            }

            // 设置shopId到查询条件中
            orderQueryDTO.setShopId(shop.getShopId());

            // 调用服务层获取订单列表
            List<Orders> orderList = orderService.getOrderList(orderQueryDTO);
            return Result.success(orderList);
        } catch (Exception e) {
            return Result.error("获取商家订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 商家获取订单详情
     * 
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("/order/{orderId}")
    public Result<Orders> getShopOrderDetail(@PathVariable Integer orderId) {
        try {
            // 调用服务层获取订单详情
            Orders order = orderService.getOrderById(orderId);
            return Result.success(order);
        } catch (Exception e) {
            return Result.error("获取商家订单详情失败：" + e.getMessage());
        }
    }

    /**
     * 商家发货
     * 
     * @param orderId       订单ID
     * @param logisticsInfo 物流信息
     * @return 发货结果
     */
    @PutMapping("/order/ship/{orderId}")
    public Result<String> shipOrder(@PathVariable Integer orderId,
            @RequestBody java.util.Map<String, String> logisticsInfo) {
        try {
            // 调用服务层发货
            // 注意：这里需要OrderService中添加shipOrder方法，并支持物流信息参数
            return Result.error("暂未实现商家发货功能");
        } catch (Exception e) {
            return Result.error("商家发货失败：" + e.getMessage());
        }
    }

    /**
     * 上传店铺logo
     * 
     * @param logo 店铺logo文件
     * @return 相对路径
     */
    @PostMapping("/logo/upload")
    public Result<String> uploadShopLogo(@RequestPart("logo") MultipartFile logo) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();

            // 生成唯一文件名
            String originalFilename = logo.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;

            // 上传文件到OSS
            String fullLogoUrl = aliyunOSSOperator.upload(logo.getBytes(), uniqueFileName);

            // 提取关键字符（去掉前缀，只保留yyyy/MM/xxx.jpg部分）
            // 从完整URL中提取objectName，格式为：yyyy/MM/UUID.xxx
            // 完整URL格式：https://bucket-name.oss-region.aliyuncs.com/yyyy/MM/UUID.xxx
            // 找到第4个斜杠的位置，即域名后的第一个斜杠
            int firstSlashIndex = fullLogoUrl.indexOf("/");
            firstSlashIndex = fullLogoUrl.indexOf("/", firstSlashIndex + 1);
            firstSlashIndex = fullLogoUrl.indexOf("/", firstSlashIndex + 1);

            // 提取从第4个斜杠开始的所有内容，即 yyyy/MM/UUID.xxx
            String objectName = fullLogoUrl.substring(firstSlashIndex + 1);

            // 更新店铺logo
            ShopUpdateDTO shopUpdateDTO = new ShopUpdateDTO();
            shopUpdateDTO.setShopLogo(fullLogoUrl);
            shopService.updateShopByMerchantId(merchantId, shopUpdateDTO);

            // 返回成功提示和关键字符
            return Result.success(objectName);
        } catch (Exception e) {
            return Result.error("上传店铺logo失败：" + e.getMessage());
        }
    }

    /**
     * 上传店铺横幅
     * 
     * @param banner 店铺横幅文件
     * @return 相对路径
     */
    @PostMapping("/banner/upload")
    public Result<String> uploadShopBanner(@RequestPart("banner") MultipartFile banner) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();

            // 生成唯一文件名
            String originalFilename = banner.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;

            // 上传文件到OSS
            String fullBannerUrl = aliyunOSSOperator.upload(banner.getBytes(), uniqueFileName);

            // 提取关键字符（去掉前缀，只保留yyyy/MM/xxx.jpg部分）
            // 从完整URL中提取objectName，格式为：yyyy/MM/UUID.xxx
            // 完整URL格式：https://bucket-name.oss-region.aliyuncs.com/yyyy/MM/UUID.xxx
            // 找到第4个斜杠的位置，即域名后的第一个斜杠
            int firstSlashIndex = fullBannerUrl.indexOf("/");
            firstSlashIndex = fullBannerUrl.indexOf("/", firstSlashIndex + 1);
            firstSlashIndex = fullBannerUrl.indexOf("/", firstSlashIndex + 1);

            // 提取从第4个斜杠开始的所有内容，即 yyyy/MM/UUID.xxx
            String objectName = fullBannerUrl.substring(firstSlashIndex + 1);

            // 更新店铺横幅
            ShopUpdateDTO shopUpdateDTO = new ShopUpdateDTO();
            shopUpdateDTO.setShopBanner(fullBannerUrl);
            shopService.updateShopByMerchantId(merchantId, shopUpdateDTO);

            // 返回成功提示和关键字符
            return Result.success(objectName);
        } catch (Exception e) {
            return Result.error("上传店铺横幅失败：" + e.getMessage());
        }
    }

    /**
     * 上传商品主图（仅上传图片，不直接关联商品）
     * 
     * @param file 上传的文件
     * @return 上传结果，包含图片的相对路径
     */
    @PostMapping("/product/main-image/upload")
    public Result<String> uploadProductMainImage(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            // 调用OSS上传工具
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());

            // 截取URL前缀，只保留相对路径部分
            // 例如：https://bucket-name.oss-region.aliyuncs.com/2025/12/xxx.jpg ->
            // 2025/12/xxx.jpg
            String relativePath = ossUrl;
            if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                // 找到第一个斜杠后的两个斜杠位置（https://），然后取后面的部分
                int startIndex = ossUrl.indexOf("//") + 2;
                // 找到域名后的第一个斜杠位置
                int pathStartIndex = ossUrl.indexOf("/", startIndex);
                if (pathStartIndex != -1) {
                    relativePath = ossUrl.substring(pathStartIndex + 1);
                }
            }

            return Result.success(relativePath);
        } catch (Exception e) {
            return Result.error("商品主图上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传商品详情图（仅上传图片，不直接关联商品）
     * 
     * @param file 上传的文件
     * @return 上传结果，包含图片的相对路径
     */
    @PostMapping("/product/detail-image/upload")
    public Result<String> uploadProductDetailImage(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            // 调用OSS上传工具
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());

            // 截取URL前缀，只保留相对路径部分
            String relativePath = ossUrl;
            if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                int startIndex = ossUrl.indexOf("//") + 2;
                int pathStartIndex = ossUrl.indexOf("/", startIndex);
                if (pathStartIndex != -1) {
                    relativePath = ossUrl.substring(pathStartIndex + 1);
                }
            }

            return Result.success(relativePath);
        } catch (Exception e) {
            return Result.error("商品详情图上传失败：" + e.getMessage());
        }
    }

    /**
     * 根据商品ID获取商品详情列表（包含SKU信息）
     * 
     * @param productId 商品ID
     * @return 商品详情列表（包含SKU信息）
     */
    @GetMapping("/products/{productId}")
    public Result<List<Product>> getProductDetailsByProductId(@PathVariable Integer productId) {
        try {
            // 创建只有一个元素的列表
            Product product = productService.findProductDetail(productId);
            List<Product> productList = new ArrayList<>();
            productList.add(product);
            return Result.success(productList);
        } catch (Exception e) {
            return Result.error("获取商品详情失败：" + e.getMessage());
        }
    }
}
