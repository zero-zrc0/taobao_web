package org.taobao.controller;

import org.taobao.pojo.Result;
import org.taobao.pojo.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.taobao.mapper.ProductMapper;
import org.taobao.mapper.ProductSkuMapper;
import org.taobao.utils.AliyunOSSOperator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    /**
     * 文件上传接口
     * 
     * @param file 上传的文件（接受名为file的字段）
     * @return 上传结果，包含文件URL
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            log.info("接收到文件上传请求");

            if (file == null || file.isEmpty()) {
                log.error("上传文件为空");
                return Result.error("上传文件不能为空");
            }

            log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());

            // 调用OSS上传工具
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());

            log.info("文件上传成功，URL：{}", ossUrl);
            return Result.success(ossUrl);
        } catch (Exception e) {
            log.error("文件上传失败：", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 支持多文件上传的接口
     * 
     * @param files 上传的文件数组
     * @return 上传结果，包含多个文件URL
     */
    @PostMapping("/upload/multiple")
    public Result<String[]> uploadMultipleFiles(@RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            log.info("接收到多文件上传请求");

            if (files == null || files.length == 0) {
                log.error("上传文件数组为空");
                return Result.error("上传文件不能为空");
            }

            log.info("上传文件数量：{}", files.length);

            String[] ossUrls = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file == null || file.isEmpty()) {
                    log.error("第{}个文件为空，跳过", i + 1);
                    ossUrls[i] = null;
                    continue;
                }

                log.info("第{}个文件：{}，大小：{}字节", i + 1, file.getOriginalFilename(), file.getSize());
                ossUrls[i] = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
                log.info("第{}个文件上传成功，URL：{}", i + 1, ossUrls[i]);
            }

            return Result.success(ossUrls);
        } catch (Exception e) {
            log.error("多文件上传失败：", e);
            return Result.error("多文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传商品图片并更新数据库
     * 
     * @param productId 商品ID
     * @param file      上传的文件
     * @param imageType 图片类型：main（主图）或detail（详情图）
     * @return 上传结果，包含完整的OSS URL
     */
    @PostMapping("/upload/product/{productId}")
    public Result<String> uploadProductImage(
            @PathVariable Integer productId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageType", defaultValue = "main") String imageType) {
        try {
            log.info("接收到商品图片上传请求，商品ID：{}，图片类型：{}", productId, imageType);

            if (file == null || file.isEmpty()) {
                log.error("上传文件为空");
                return Result.error("上传文件不能为空");
            }

            // 1. 上传图片到OSS
            log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
            log.info("文件上传成功，完整URL：{}", ossUrl);

            // 2. 截取URL前缀，只保留相对路径部分
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
            log.info("截取后的相对路径：{}", relativePath);

            // 3. 更新商品数据库
            Product product = productMapper.findById(productId);
            if (product == null) {
                log.error("商品不存在，商品ID：{}", productId);
                return Result.error("商品不存在");
            }

            Date now = new Date();
            if ("main".equals(imageType)) {
                // 更新主图，使用简单的字符串拼接生成JSON数组
                product.setMainImages("[" + relativePath + "]");
            } else if ("detail".equals(imageType)) {
                // 更新详情图，使用简单的字符串拼接生成JSON数组
                product.setDetailImages("[" + relativePath + "]");
            } else {
                log.error("无效的图片类型：{}", imageType);
                return Result.error("无效的图片类型，支持main或detail");
            }
            product.setUpdateTime(now);

            // 更新商品信息
            productMapper.update(product);
            log.info("商品图片更新成功，商品ID：{}", productId);

            // 4. 返回完整的OSS URL
            return Result.success(ossUrl);
        } catch (Exception e) {
            log.error("商品图片上传失败：", e);
            return Result.error("商品图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传多张商品图片，返回特定格式的URL字符串
     * 
     * @param files 上传的文件数组
     * @return 上传结果，包含格式为 " `url1` `url2` " 的字符串
     */
    @PostMapping("/upload/product/format")
    public Result<String> uploadProductImageFormat(
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            log.info("接收到商品图片格式化上传请求");

            if (files == null || files.length == 0) {
                log.error("上传文件数组为空");
                return Result.error("上传文件不能为空");
            }

            // 1. 上传所有图片到OSS，获取URL列表
            List<String> ossUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    log.warn("跳过空文件");
                    continue;
                }

                log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());
                String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
                log.info("文件上传成功，完整URL：{}", ossUrl);
                ossUrls.add(ossUrl);
            }

            if (ossUrls.isEmpty()) {
                log.error("没有成功上传的文件");
                return Result.error("没有成功上传的文件");
            }

            // 2. 构建符合要求的响应格式：" `url1` `url2` "
            StringBuilder formattedUrls = new StringBuilder();
            for (String url : ossUrls) {
                formattedUrls.append(" `").append(url).append("` ");
            }
            // 去除首尾空格
            String resultUrls = formattedUrls.toString().trim();

            // 3. 返回格式化的URL字符串
            return Result.success(resultUrls);
        } catch (Exception e) {
            log.error("商品图片格式化上传失败：", e);
            return Result.error("商品图片格式化上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传商品主图（仅上传图片，不直接关联商品）
     * 
     * @param file 上传的文件
     * @return 上传结果，包含图片的相对路径
     */
    @PostMapping("/upload/product/main-image")
    public Result<String> uploadProductMainImage(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            log.info("接收到商品主图上传请求");

            if (file == null || file.isEmpty()) {
                log.error("上传文件为空");
                return Result.error("上传文件不能为空");
            }

            log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());

            // 调用OSS上传工具
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());

            log.info("文件上传成功，URL：{}", ossUrl);

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
            log.info("截取后的相对路径：{}", relativePath);

            return Result.success(relativePath);
        } catch (Exception e) {
            log.error("商品主图上传失败：", e);
            return Result.error("商品主图上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传商品详情图（仅上传图片，不直接关联商品）
     * 
     * @param file 上传的文件
     * @return 上传结果，包含图片的相对路径
     */
    @PostMapping("/upload/product/detail-image")
    public Result<String> uploadProductDetailImage(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            log.info("接收到商品详情图上传请求");

            if (file == null || file.isEmpty()) {
                log.error("上传文件为空");
                return Result.error("上传文件不能为空");
            }

            log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());

            // 调用OSS上传工具
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());

            log.info("文件上传成功，URL：{}", ossUrl);

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
            log.info("截取后的相对路径：{}", relativePath);

            return Result.success(relativePath);
        } catch (Exception e) {
            log.error("商品详情图上传失败：", e);
            return Result.error("商品详情图上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传商品主图（仅上传图片，不直接关联商品）
     * 
     * @param files 上传的文件数组
     * @return 上传结果，包含图片相对路径的列表
     */
    @PostMapping("/upload/product/main-images")
    public Result<List<String>> uploadProductMainImages(
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            log.info("接收到批量商品主图上传请求");

            if (files == null || files.length == 0) {
                log.error("上传文件数组为空");
                return Result.error("上传文件不能为空");
            }

            List<String> relativePaths = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    log.warn("跳过空文件");
                    continue;
                }

                log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());

                // 调用OSS上传工具
                String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
                log.info("文件上传成功，URL：{}", ossUrl);

                // 截取URL前缀，只保留相对路径部分
                String relativePath = ossUrl;
                if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                    int startIndex = ossUrl.indexOf("//") + 2;
                    int pathStartIndex = ossUrl.indexOf("/", startIndex);
                    if (pathStartIndex != -1) {
                        relativePath = ossUrl.substring(pathStartIndex + 1);
                    }
                }
                log.info("截取后的相对路径：{}", relativePath);

                relativePaths.add(relativePath);
            }

            if (relativePaths.isEmpty()) {
                log.error("没有成功上传的文件");
                return Result.error("没有成功上传的文件");
            }

            return Result.success(relativePaths);
        } catch (Exception e) {
            log.error("批量商品主图上传失败：", e);
            return Result.error("批量商品主图上传失败：" + e.getMessage());
        }
    }

    /**
     * 批量上传商品详情图（仅上传图片，不直接关联商品）
     * 
     * @param files 上传的文件数组
     * @return 上传结果，包含图片相对路径的列表
     */
    @PostMapping("/upload/product/detail-images")
    public Result<List<String>> uploadProductDetailImages(
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            log.info("接收到批量商品详情图上传请求");

            if (files == null || files.length == 0) {
                log.error("上传文件数组为空");
                return Result.error("上传文件不能为空");
            }

            List<String> relativePaths = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    log.warn("跳过空文件");
                    continue;
                }

                log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());

                // 调用OSS上传工具
                String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
                log.info("文件上传成功，URL：{}", ossUrl);

                // 截取URL前缀，只保留相对路径部分
                String relativePath = ossUrl;
                if (ossUrl.startsWith("http://") || ossUrl.startsWith("https://")) {
                    int startIndex = ossUrl.indexOf("//") + 2;
                    int pathStartIndex = ossUrl.indexOf("/", startIndex);
                    if (pathStartIndex != -1) {
                        relativePath = ossUrl.substring(pathStartIndex + 1);
                    }
                }
                log.info("截取后的相对路径：{}", relativePath);

                relativePaths.add(relativePath);
            }

            if (relativePaths.isEmpty()) {
                log.error("没有成功上传的文件");
                return Result.error("没有成功上传的文件");
            }

            return Result.success(relativePaths);
        } catch (Exception e) {
            log.error("批量商品详情图上传失败：", e);
            return Result.error("批量商品详情图上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传SKU图片并直接更新数据库
     * 
     * @param skuId SKU ID
     * @param file  上传的文件
     * @return 上传结果，包含完整的OSS URL
     */
    @PostMapping("/upload/sku/{skuId}")
    public Result<String> uploadSkuImage(
            @PathVariable Integer skuId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            log.info("接收到SKU图片上传请求，SKU ID：{}", skuId);

            if (file == null || file.isEmpty()) {
                log.error("上传文件为空");
                return Result.error("上传文件不能为空");
            }

            // 1. 上传图片到OSS
            log.info("上传文件名：{}，大小：{}字节", file.getOriginalFilename(), file.getSize());
            String ossUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
            log.info("文件上传成功，完整URL：{}", ossUrl);

            // 2. 截取URL前缀，只保留相对路径部分
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
            log.info("截取后的相对路径：{}", relativePath);

            // 3. 更新SKU数据库
            // 先查询现有SKU信息
            org.taobao.pojo.ProductSku existingSku = productSkuMapper.findById(skuId);
            if (existingSku == null) {
                log.error("SKU不存在，SKU ID：{}", skuId);
                return Result.error("SKU不存在");
            }

            // 只更新SKU图片和更新时间
            existingSku.setSkuImage(relativePath);
            existingSku.setUpdateTime(new Date());

            // 更新SKU信息
            productSkuMapper.update(existingSku);
            log.info("SKU图片更新成功，SKU ID：{}", skuId);

            // 4. 返回完整的OSS URL
            return Result.success(ossUrl);
        } catch (Exception e) {
            log.error("SKU图片上传失败：", e);
            return Result.error("SKU图片上传失败：" + e.getMessage());
        }
    }
}