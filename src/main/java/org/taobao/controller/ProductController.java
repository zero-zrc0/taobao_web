package org.taobao.controller;

import org.taobao.context.BaseContext;
import org.taobao.dto.HomeProductQueryDTO;
import org.taobao.dto.ProductCreateDTO;
import org.taobao.dto.ProductQueryDTO;
import org.taobao.dto.ProductUpdateDTO;
import org.taobao.dto.ProductSkuDTO;
import org.taobao.pojo.Result;
import org.taobao.pojo.Product;
import org.taobao.pojo.ProductSku;
import org.taobao.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.taobao.utils.AliyunOSSOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 商品Controller
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 根据SKU ID获取SKU信息
     * 
     * @param skuId SKU ID
     * @return SKU信息
     */
    @GetMapping("/sku/{skuId}")
    public Result<ProductSkuDTO> getSkuById(@PathVariable Integer skuId) {
        try {
            ProductSku sku = productService.getSkuById(skuId);
            ProductSkuDTO skuDTO = ProductSkuDTO.fromProductSku(sku);
            return Result.success(skuDTO);
        } catch (Exception e) {
            return Result.error("获取SKU信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取商品列表
     * 
     * @param productQueryDTO 查询条件
     * @return 商品列表
     */
    @GetMapping("/list")
    public Result<List<Product>> getProductList(ProductQueryDTO productQueryDTO) {
        try {
            List<Product> productList = productService.getProductList(productQueryDTO);
            return Result.success(productList);
        } catch (Exception e) {
            return Result.error("获取商品列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取店铺商品列表
     * 
     * @param productQueryDTO 查询条件
     * @return 商品列表
     */
    @GetMapping("/shop/list")
    public Result<List<Product>> getShopProductList(ProductQueryDTO productQueryDTO) {
        try {
            // 从BaseContext中获取当前用户ID
            Integer merchantId = BaseContext.getCurrentId().intValue();
            // 这里可以根据merchantId查询店铺ID，然后调用getShopProductList方法
            // 暂时假设前端传递了shopId参数
            List<Product> productList = productService.getShopProductList(productQueryDTO.getShopId(), productQueryDTO);
            return Result.success(productList);
        } catch (Exception e) {
            return Result.error("获取店铺商品列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建商品（支持图片上传）
     * 
     * @param productCreateDTO 商品创建信息
     * @param mainImageFiles   商品主图文件数组
     * @param detailImageFiles 商品详情图文件数组
     * @param skuImageFiles    SKU图片文件数组（与SKU列表一一对应）
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<String> createProduct(
            @RequestPart("product") ProductCreateDTO productCreateDTO,
            @RequestPart(value = "mainImageFiles", required = false) MultipartFile[] mainImageFiles,
            @RequestPart(value = "detailImageFiles", required = false) MultipartFile[] detailImageFiles,
            @RequestPart(value = "skuImageFiles", required = false) MultipartFile[] skuImageFiles) {
        try {
            // 上传主图
            if (mainImageFiles != null && mainImageFiles.length > 0) {
                List<String> mainImageUrls = new ArrayList<>();
                for (MultipartFile file : mainImageFiles) {
                    if (file != null && !file.isEmpty()) {
                        String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
                        // 提取相对路径
                        String relativePath = extractRelativePath(ossUrl);
                        mainImageUrls.add(relativePath);
                    }
                }
                // 将主图URL列表转换为字符串（用逗号分隔）
                if (!mainImageUrls.isEmpty()) {
                    productCreateDTO.setMainImages(String.join(",", mainImageUrls));
                }
            }

            // 上传详情图
            if (detailImageFiles != null && detailImageFiles.length > 0) {
                List<String> detailImageUrls = new ArrayList<>();
                for (MultipartFile file : detailImageFiles) {
                    if (file != null && !file.isEmpty()) {
                        String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
                        // 提取相对路径
                        String relativePath = extractRelativePath(ossUrl);
                        detailImageUrls.add(relativePath);
                    }
                }
                // 将详情图URL列表转换为字符串（用逗号分隔）
                if (!detailImageUrls.isEmpty()) {
                    productCreateDTO.setDetailImages(String.join(",", detailImageUrls));
                }
            }

            // 处理SKU图片
            List<String> skuImagePaths = new ArrayList<>();
            if (productCreateDTO.getSkus() != null && !productCreateDTO.getSkus().isEmpty() &&
                    skuImageFiles != null && skuImageFiles.length > 0) {
                // 上传SKU图片并收集路径
                for (MultipartFile skuImageFile : skuImageFiles) {
                    if (skuImageFile != null && !skuImageFile.isEmpty()) {
                        String ossUrl = aliyunOSSOperator.upload(
                                skuImageFile.getBytes(),
                                skuImageFile.getOriginalFilename());
                        // 提取相对路径
                        String relativePath = extractRelativePath(ossUrl);
                        skuImagePaths.add(relativePath);
                    } else {
                        skuImagePaths.add(null); // 占位符，保证索引一致性
                    }
                }
            }

            // 调用服务层创建商品和SKU（包含SKU图片），整个过程在事务控制下进行
            productService.createProductWithSkuImages(productCreateDTO, skuImagePaths);

            return Result.success("商品创建成功");
        } catch (Exception e) {
            return Result.error("商品创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新商品
     * 
     * @param productId        商品ID
     * @param productUpdateDTO 商品更新信息
     * @return 更新结果
     */
    @PutMapping("/update/{productId}")
    public Result<String> updateProduct(@PathVariable Integer productId,
            @RequestBody ProductUpdateDTO productUpdateDTO) {
        try {
            productService.updateProduct(productId, productUpdateDTO);
            return Result.success("商品更新成功");
        } catch (Exception e) {
            return Result.error("商品更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除商品
     * 
     * @param productId 商品ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{productId}")
    public Result<String> deleteProduct(@PathVariable Integer productId) {
        try {
            productService.deleteProduct(productId);
            return Result.success("商品删除成功");
        } catch (Exception e) {
            return Result.error("商品删除失败：" + e.getMessage());
        }
    }

    /**
     * 商品上架
     * 
     * @param productId 商品ID
     * @return 上架结果
     */
    @PutMapping("/on-sale/{productId}")
    public Result<String> onSaleProduct(@PathVariable Integer productId) {
        try {
            productService.onSaleProduct(productId);
            return Result.success("商品上架成功");
        } catch (Exception e) {
            return Result.error("商品上架失败：" + e.getMessage());
        }
    }

    /**
     * 商品下架
     * 
     * @param productId 商品ID
     * @return 下架结果
     */
    @PutMapping("/off-sale/{productId}")
    public Result<String> offSaleProduct(@PathVariable Integer productId) {
        try {
            productService.offSaleProduct(productId);
            return Result.success("商品下架成功");
        } catch (Exception e) {
            return Result.error("商品下架失败：" + e.getMessage());
        }
    }

    /**
     * 获取商品SKU列表
     * 
     * @param productId 商品ID
     * @return SKU列表
     */
    @GetMapping("/skus/{productId}")
    public Result<List<ProductSkuDTO>> getProductSkus(@PathVariable Integer productId) {
        try {
            List<ProductSku> skus = productService.getProductSkus(productId);
            // 转换为ProductSkuDTO列表
            List<ProductSkuDTO> skuDTOs = skus.stream()
                    .map(ProductSkuDTO::fromProductSku)
                    .toList();
            return Result.success(skuDTOs);
        } catch (Exception e) {
            return Result.error("获取商品SKU列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取首页商品列表（支持分类筛选和模糊查询，按数量返回）
     * 
     * @param categoryId  分类ID：1-数码, 2-生鲜, 3-图书, 4-衣服, 5-零食, 6-宠物
     * @param productName 商品名称，用于模糊查询
     * @param limit       返回数量，默认18（3排，每排6个）
     * @return 随机排列的商品列表
     */
    @GetMapping("/home/list")
    public Result<List<Product>> getHomeProductList(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String productName,
            @RequestParam(defaultValue = "18") Integer limit) {
        try {
            // 构建查询DTO
            HomeProductQueryDTO homeProductQueryDTO = new HomeProductQueryDTO();
            homeProductQueryDTO.setCategoryId(categoryId);
            homeProductQueryDTO.setProductName(productName);
            homeProductQueryDTO.setLimit(limit);

            // 调用服务层获取首页商品列表
            List<Product> productList = productService.getHomeProductList(homeProductQueryDTO);
            return Result.success(productList);
        } catch (Exception e) {
            return Result.error("获取首页商品列表失败：" + e.getMessage());
        }
    }

    /**
     * 上传SKU图片
     * 
     * @param skuImage SKU图片文件
     * @return 相对路径
     */
    @PostMapping("/sku/image/upload")
    public Result<String> uploadSkuImage(@RequestPart(value = "skuImage", required = false) MultipartFile skuImage) {
        try {
            if (skuImage == null || skuImage.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            // 生成唯一文件名
            String originalFilename = skuImage.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extName;

            // 上传文件到OSS
            String fullImageUrl = aliyunOSSOperator.upload(skuImage.getBytes(), uniqueFileName);

            // 提取关键字符（去掉前缀，只保留yyyy/MM/xxx.jpg部分）
            // 从完整URL中提取objectName，格式为：yyyy/MM/UUID.xxx
            // 完整URL格式：https://bucket-name.oss-region.aliyuncs.com/yyyy/MM/UUID.xxx
            // 找到第4个斜杠的位置，即域名后的第一个斜杠
            int firstSlashIndex = fullImageUrl.indexOf("/");
            firstSlashIndex = fullImageUrl.indexOf("/", firstSlashIndex + 1);
            firstSlashIndex = fullImageUrl.indexOf("/", firstSlashIndex + 1);

            // 提取从第4个斜杠开始的所有内容，即 yyyy/MM/UUID.xxx
            String objectName = fullImageUrl.substring(firstSlashIndex + 1);

            // 返回成功提示和关键字符
            return Result.success(objectName);
        } catch (Exception e) {
            return Result.error("上传SKU图片失败：" + e.getMessage());
        }
    }

    /**
     * 根据商品ID获取商品详情（包含SKU信息）
     * 
     * @param productId 商品ID
     * @return 商品详情（包含SKU信息）
     */
    @GetMapping("/detail/{productId}")
    public Result<Product> getProductById(@PathVariable Integer productId) {
        try {
            Product product = productService.findProductDetail(productId);
            return Result.success(product);
        } catch (Exception e) {
            return Result.error("获取商品详情失败：" + e.getMessage());
        }
    }

    /**
     * 从完整OSS URL中提取相对路径
     * 
     * @param ossUrl 完整的OSS URL
     * @return 相对路径
     */
    private String extractRelativePath(String ossUrl) {
        if (ossUrl == null || ossUrl.isEmpty()) {
            return ossUrl;
        }

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
        return relativePath;
    }
}