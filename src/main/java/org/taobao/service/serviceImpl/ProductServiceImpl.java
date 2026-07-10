package org.taobao.service.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.taobao.context.BaseContext;
import org.taobao.dto.HomeProductQueryDTO;
import org.taobao.dto.ProductCreateDTO;
import org.taobao.dto.ProductQueryDTO;
import org.taobao.dto.ProductUpdateDTO;
import org.taobao.dto.ProductSkuCreateDTO;
import org.taobao.dto.ProductSkuUpdateDTO;
import org.taobao.exception.ProductNotFoundException;
import org.taobao.mapper.ProductMapper;
import org.taobao.mapper.ProductSkuMapper;
import org.taobao.pojo.Product;
import org.taobao.pojo.ProductSku;
import org.taobao.service.ProductService;
import org.taobao.utils.OssUrlUtils;
import org.taobao.utils.AliyunOSSOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 商品Service实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Override
    public ProductSku getSkuById(Integer skuId) {
        ProductSku sku = productSkuMapper.findById(skuId);
        if (sku == null) {
            throw new ProductNotFoundException("SKU不存在");
        }
        fillSkuImageUrls(List.of(sku));
        return sku;
    }

    @Override
    public List<Product> getProductList(ProductQueryDTO productQueryDTO) {
        // 强制重新计算偏移量，不受前端参数影响
        if (productQueryDTO.getPageNum() != null && productQueryDTO.getPageSize() != null) {
            productQueryDTO.setOffset((productQueryDTO.getPageNum() - 1) * productQueryDTO.getPageSize());
        } else {
            // 如果没有页码信息，默认从第0条开始
            productQueryDTO.setOffset(0);
        }

        // 验证和修正排序参数，防止SQL注入和语法错误
        if (productQueryDTO.getSortBy() == null || productQueryDTO.getSortBy().trim().isEmpty()) {
            productQueryDTO.setSortBy("create_time");
        }

        // 确保排序方式只有ASC或DESC两种值
        if (productQueryDTO.getSortOrder() == null ||
                (!"asc".equalsIgnoreCase(productQueryDTO.getSortOrder().trim()) &&
                        !"desc".equalsIgnoreCase(productQueryDTO.getSortOrder().trim()))) {
            productQueryDTO.setSortOrder("desc");
        }

        List<Product> products = productMapper.getProductList(productQueryDTO);
        // 为每个商品设置第一个SKU的价格
        for (Product product : products) {
            List<ProductSku> skus = productSkuMapper.findByProductId(product.getProductId());
            if (skus != null && !skus.isEmpty()) {
                product.setPrice(skus.get(0).getPrice());
            }
        }
        fillImageUrls(products);
        return products;
    }

    @Override
    @Transactional
    public Product createProduct(ProductCreateDTO productCreateDTO) {
        // 从BaseContext中获取当前用户ID
        Integer merchantId = BaseContext.getCurrentId().intValue();

        // 创建商品对象
        Product product = new Product();
        product.setProductName(productCreateDTO.getProductName());
        product.setDescription(productCreateDTO.getDescription());

        // 处理主图和详情图，去除OSS前缀并直接存储
        // 处理主图
        String mainImages = productCreateDTO.getMainImages();
        if (mainImages != null && !mainImages.isEmpty()) {
            product.setMainImages(OssUrlUtils.extractObjectKeys(mainImages));
        }

        // 处理详情图
        String detailImages = productCreateDTO.getDetailImages();
        if (detailImages != null && !detailImages.isEmpty()) {
            product.setDetailImages(OssUrlUtils.extractObjectKeys(detailImages));
        }

        product.setCategoryId(productCreateDTO.getCategoryId());
        product.setMerchantId(merchantId);
        product.setShopId(productCreateDTO.getShopId());
        product.setStatus("on_sale"); // 默认上架状态

        Date now = new Date();
        product.setCreateTime(now);
        product.setUpdateTime(now);

        // 插入商品
        productMapper.insert(product);

        // 插入商品SKU
        if (productCreateDTO.getSkus() != null && !productCreateDTO.getSkus().isEmpty()) {
            for (ProductSkuCreateDTO skuCreateDTO : productCreateDTO.getSkus()) {
                ProductSku productSku = new ProductSku();
                productSku.setProductId(product.getProductId());
                productSku.setSkuName(skuCreateDTO.getSkuName());
                productSku.setSkuType(skuCreateDTO.getSkuType());
                productSku.setPrice(skuCreateDTO.getPrice());
                // 处理可选的库存数量，默认为50
                productSku.setStock(skuCreateDTO.getStock() != null ? skuCreateDTO.getStock() : 50);

                String skuImage = skuCreateDTO.getSkuImage();
                productSku.setSkuImage(OssUrlUtils.extractObjectKey(skuImage));

                productSku.setStatus("on_sale"); // 默认上架状态
                productSku.setCreateTime(now);
                productSku.setUpdateTime(now);

                // 插入SKU
                productSkuMapper.insert(productSku);
            }
        }

        // 返回创建的商品对象
        return product;
    }

    @Override
    public void updateProduct(Integer productId, ProductUpdateDTO productUpdateDTO) {
        // 检查商品是否存在
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException("商品不存在");
        }

        // 更新商品基本信息
        product.setProductName(productUpdateDTO.getProductName());
        product.setDescription(productUpdateDTO.getDescription());

        // 处理主图和详情图，去除OSS前缀并直接存储
        // 处理主图
        String mainImages = productUpdateDTO.getMainImages();
        if (mainImages != null && !mainImages.isEmpty()) {
            product.setMainImages(OssUrlUtils.extractObjectKeys(mainImages));
        } else {
            product.setMainImages(null);
        }

        // 处理详情图
        String detailImages = productUpdateDTO.getDetailImages();
        if (detailImages != null && !detailImages.isEmpty()) {
            product.setDetailImages(OssUrlUtils.extractObjectKeys(detailImages));
        } else {
            product.setDetailImages(null);
        }

        product.setCategoryId(productUpdateDTO.getCategoryId());
        product.setStatus(productUpdateDTO.getStatus());
        product.setUpdateTime(new Date());

        // 更新商品
        productMapper.update(product);

        // 更新商品SKU
        if (productUpdateDTO.getSkus() != null && !productUpdateDTO.getSkus().isEmpty()) {
            for (ProductSkuUpdateDTO skuUpdateDTO : productUpdateDTO.getSkus()) {
                ProductSku productSku = productSkuMapper.findById(skuUpdateDTO.getSkuId());
                if (productSku == null) {
                    throw new ProductNotFoundException("SKU不存在");
                }

                // 更新SKU信息
                productSku.setSkuName(skuUpdateDTO.getSkuName());
                productSku.setSkuType(skuUpdateDTO.getSkuType());
                productSku.setPrice(skuUpdateDTO.getPrice());

                // 处理可选的库存数量，只有当提供了新的库存值时才更新
                if (skuUpdateDTO.getStock() != null) {
                    productSku.setStock(skuUpdateDTO.getStock());
                }

                String skuImage = skuUpdateDTO.getSkuImage();
                productSku.setSkuImage(OssUrlUtils.extractObjectKey(skuImage));

                productSku.setStatus(skuUpdateDTO.getStatus());
                productSku.setUpdateTime(new Date());

                // 更新SKU
                productSkuMapper.update(productSku);
            }
        }
    }

    @Override
    public void deleteProduct(Integer productId) {
        // 检查商品是否存在
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException("商品不存在");
        }

        // 删除商品SKU
        productSkuMapper.deleteByProductId(productId);

        // 删除商品
        productMapper.delete(productId);
    }

    @Override
    public void onSaleProduct(Integer productId) {
        // 检查商品是否存在
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException("商品不存在");
        }

        // 更新商品状态为上架
        productMapper.updateStatus(productId, "on_sale");

        // 更新商品SKU状态为上架
        productSkuMapper.updateStatusByProductId(productId, "on_sale");
    }

    @Override
    public void offSaleProduct(Integer productId) {
        // 检查商品是否存在
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException("商品不存在");
        }

        // 更新商品状态为下架
        productMapper.updateStatus(productId, "off_sale");

        // 更新商品SKU状态为下架
        productSkuMapper.updateStatusByProductId(productId, "off_sale");
    }

    @Override
    public List<Product> getShopProductList(Integer shopId, ProductQueryDTO productQueryDTO) {
        // 强制重新计算偏移量，不受前端参数影响
        if (productQueryDTO.getPageNum() != null && productQueryDTO.getPageSize() != null) {
            productQueryDTO.setOffset((productQueryDTO.getPageNum() - 1) * productQueryDTO.getPageSize());
        } else {
            // 如果没有页码信息，默认从第0条开始
            productQueryDTO.setOffset(0);
        }

        // 验证和修正排序参数，防止SQL注入和语法错误
        if (productQueryDTO.getSortBy() == null || productQueryDTO.getSortBy().trim().isEmpty()) {
            productQueryDTO.setSortBy("create_time");
        }

        // 确保排序方式只有ASC或DESC两种值
        if (productQueryDTO.getSortOrder() == null ||
                (!"asc".equalsIgnoreCase(productQueryDTO.getSortOrder().trim()) &&
                        !"desc".equalsIgnoreCase(productQueryDTO.getSortOrder().trim()))) {
            productQueryDTO.setSortOrder("desc");
        }

        // 设置店铺ID到查询条件中
        productQueryDTO.setShopId(shopId);
        List<Product> products = productMapper.getShopProductList(productQueryDTO);
        fillImageUrls(products);
        return products;
    }

    @Override
    public List<ProductSku> getProductSkus(Integer productId) {
        List<ProductSku> skus = productSkuMapper.findByProductId(productId);
        fillSkuImageUrls(skus);
        return skus;
    }

    @Override
    public List<Product> getHomeProductList(HomeProductQueryDTO homeProductQueryDTO) {
        // 设置默认返回数量为18（3排，每排6个）
        if (homeProductQueryDTO.getLimit() == null || homeProductQueryDTO.getLimit() <= 0) {
            homeProductQueryDTO.setLimit(18);
        }

        // 获取数据库中符合条件的商品
        List<Product> products = productMapper.getHomeProductList(homeProductQueryDTO);

        // 为每个商品设置第一个SKU的价格
        for (Product product : products) {
            // 获取商品的所有SKU
            List<ProductSku> skus = productSkuMapper.findByProductId(product.getProductId());
            if (skus != null && !skus.isEmpty()) {
                // 设置第一个SKU的价格
                product.setPrice(skus.get(0).getPrice());
            }
        }

        // 直接返回查询到的商品，不进行重复填充
        // 对结果进行随机排序
        Collections.shuffle(products);
        fillImageUrls(products);
        return products;
    }

    @Override
    public Product getProductById(Integer productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException("商品不存在");
        }
        // 设置第一个SKU的价格
        List<ProductSku> skus = productSkuMapper.findByProductId(product.getProductId());
        if (skus != null && !skus.isEmpty()) {
            product.setPrice(skus.get(0).getPrice());
        }
        fillImageUrls(List.of(product));
        return product;
    }

    @Override
    public Product findProductDetail(Integer productId) {
        Product product = productMapper.findProductDetail(productId);
        if (product == null) {
            throw new ProductNotFoundException("商品不存在");
        }
        fillImageUrls(List.of(product));
        return product;
    }

    @Override
    public void addSku(ProductSku productSku) {
        // 设置创建时间和更新时间
        Date now = new Date();
        productSku.setCreateTime(now);
        productSku.setUpdateTime(now);

        // 如果未设置状态，默认为on_sale
        if (productSku.getStatus() == null || productSku.getStatus().isEmpty()) {
            productSku.setStatus("on_sale");
        }

        // 插入SKU
        productSkuMapper.insert(productSku);
    }

    @Override
    public void updateSku(Integer skuId, ProductSku productSku) {
        // 检查SKU是否存在
        ProductSku existingSku = productSkuMapper.findById(skuId);
        if (existingSku == null) {
            throw new ProductNotFoundException("SKU不存在");
        }

        // 更新SKU信息
        existingSku.setSkuName(productSku.getSkuName());
        existingSku.setSkuType(productSku.getSkuType());
        existingSku.setPrice(productSku.getPrice());
        existingSku.setStock(productSku.getStock());
        existingSku.setSkuImage(OssUrlUtils.extractObjectKey(productSku.getSkuImage()));
        existingSku.setStatus(productSku.getStatus());
        existingSku.setUpdateTime(new Date());

        // 更新SKU
        productSkuMapper.update(existingSku);
    }

    @Override
    public void updateSkuImagesForNewProduct(Integer productId, List<String> skuImagePaths) {
        // 获取该商品的所有SKU
        List<ProductSku> skus = productSkuMapper.findByProductId(productId);

        // 更新SKU图片
        if (skus != null && !skus.isEmpty() && skuImagePaths != null && !skuImagePaths.isEmpty()) {
            int minSize = Math.min(skus.size(), skuImagePaths.size());
            for (int i = 0; i < minSize; i++) {
                ProductSku sku = skus.get(i);
                String imagePath = skuImagePaths.get(i);

                if (imagePath != null) {
                    sku.setSkuImage(OssUrlUtils.extractObjectKey(imagePath));
                    sku.setUpdateTime(new Date());
                    productSkuMapper.update(sku);
                }
            }
        }
    }

    @Override
    public void deleteSku(Integer skuId) {
        // 检查SKU是否存在
        ProductSku existingSku = productSkuMapper.findById(skuId);
        if (existingSku == null) {
            throw new ProductNotFoundException("SKU不存在");
        }

        // 删除SKU
        productSkuMapper.delete(skuId);
    }

    @Override
    public List<Product> getProductDetailsByShopId(Integer shopId) {
        List<Product> products = productMapper.getProductDetailsByShopId(shopId);
        fillImageUrls(products);
        return products;
    }

    private void fillImageUrls(List<Product> products) {
        if (products == null) {
            return;
        }
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            product.setMainImages(toFullImageUrls(product.getMainImages()));
            product.setDetailImages(toFullImageUrls(product.getDetailImages()));
            fillSkuImageUrls(product.getSkus());
        }
    }

    private void fillSkuImageUrls(List<ProductSku> skus) {
        if (skus == null) {
            return;
        }
        for (ProductSku sku : skus) {
            if (sku != null && sku.getSkuImage() != null && !sku.getSkuImage().isBlank()) {
                sku.setSkuImage(aliyunOSSOperator.generateSignedUrl(sku.getSkuImage()));
            }
        }
    }

    private String toFullImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.isBlank()) {
            return imageUrls;
        }

        String trimmedImageUrls = imageUrls.trim();
        if (trimmedImageUrls.startsWith("[") && trimmedImageUrls.endsWith("]")) {
            try {
                List<String> imagePaths = objectMapper.readValue(trimmedImageUrls, new TypeReference<List<String>>() {});
                return objectMapper.writeValueAsString(imagePaths.stream()
                        .map(aliyunOSSOperator::generateSignedUrl)
                        .toList());
            } catch (JsonProcessingException e) {
                // Fall through to support legacy comma-separated values.
            }
        }

        return java.util.Arrays.stream(imageUrls.split("\\s*,\\s*"))
                .map(aliyunOSSOperator::generateSignedUrl)
                .collect(java.util.stream.Collectors.joining(","));
    }

    @Override
    @Transactional
    public Product createProductWithSkuImages(ProductCreateDTO productCreateDTO, List<String> skuImagePaths) {
        // 先创建商品和SKU（不含SKU图片）
        Product product = this.createProduct(productCreateDTO);

        // 如果提供了SKU图片路径，则更新SKU图片
        if (productCreateDTO.getSkus() != null && !productCreateDTO.getSkus().isEmpty() &&
                skuImagePaths != null && !skuImagePaths.isEmpty()) {
            this.updateSkuImagesForNewProduct(product.getProductId(), skuImagePaths);
        }

        return product;
    }
}
