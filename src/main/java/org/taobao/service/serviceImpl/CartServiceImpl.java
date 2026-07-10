package org.taobao.service.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.taobao.context.BaseContext;
import org.taobao.dto.CartItemAddDTO;
import org.taobao.dto.CartItemUpdateDTO;
import org.taobao.mapper.CartMapper;
import org.taobao.mapper.ProductMapper;
import org.taobao.mapper.ProductSkuMapper;
import org.taobao.pojo.CartItem;
import org.taobao.pojo.Product;
import org.taobao.pojo.ProductSku;
import org.taobao.service.CartService;
import org.taobao.utils.AliyunOSSOperator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 购物车Service实现类
 */
@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Override
    public void addCartItem(CartItemAddDTO cartItemAddDTO) {
        // 从BaseContext中获取当前用户ID
        Long userIdLong = BaseContext.getCurrentId();
        log.info("添加购物车 - 当前用户ID(Long): {}", userIdLong);

        if (userIdLong == null) {
            log.error("添加购物车 - 用户ID为空");
            throw new RuntimeException("用户未登录");
        }

        Integer userId = userIdLong.intValue();
        log.info("添加购物车 - 当前用户ID(Integer): {}", userId);

        // 检查购物车中是否已存在该商品
        log.info("添加购物车 - 准备查询是否存在商品，userId={}, skuId={}", userId, cartItemAddDTO.getSkuId());
        CartItem existingItem = cartMapper.findByUserIdAndSkuId(userId, cartItemAddDTO.getSkuId());
        log.info("添加购物车 - 查询结果: 商品{}{}", existingItem != null ? "已存在，数量={}" : "不存在",
                existingItem != null ? existingItem.getQuantity() : "");

        Date now = new Date();

        if (existingItem != null) {
            // 已存在，更新数量
            int newQuantity = existingItem.getQuantity() + cartItemAddDTO.getQuantity();
            log.info("添加购物车 - 更新数量: 原数量={}, 新增数量={}, 新数量={}", existingItem.getQuantity(), cartItemAddDTO.getQuantity(),
                    newQuantity);
            cartMapper.updateQuantity(existingItem.getCartItemId(), newQuantity, now);
            log.info("添加购物车 - 数量更新成功");
        } else {
            // 不存在，添加新购物车项
            log.info("添加购物车 - 添加新购物车项: userId={}, skuId={}, quantity={}", userId, cartItemAddDTO.getSkuId(),
                    cartItemAddDTO.getQuantity());
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setSkuId(cartItemAddDTO.getSkuId());
            cartItem.setQuantity(cartItemAddDTO.getQuantity());
            cartItem.setCreateTime(now);
            cartItem.setUpdateTime(now);
            cartMapper.insert(cartItem);
            log.info("添加购物车 - 新购物车项添加成功");
        }
    }

    @Override
    public void updateCartItem(CartItemUpdateDTO cartItemUpdateDTO) {
        Date now = new Date();

        // 如果传入了新的SKU ID，则需要修改商品规格
        if (cartItemUpdateDTO.getSkuId() != null) {
            // 从BaseContext中获取当前用户ID
            Integer userId = BaseContext.getCurrentId().intValue();

            // 检查新的SKU是否已经存在于购物车中
            CartItem existingItem = cartMapper.findByUserIdAndSkuId(userId, cartItemUpdateDTO.getSkuId());

            // 如果数量为null，使用原数量
            Integer quantity = cartItemUpdateDTO.getQuantity();
            if (quantity == null) {
                // 获取原购物车项的数量
                CartItem originalItem = cartMapper.findById(cartItemUpdateDTO.getCartItemId());
                quantity = originalItem != null ? originalItem.getQuantity() : 1;
            }

            if (existingItem != null && !existingItem.getCartItemId().equals(cartItemUpdateDTO.getCartItemId())) {
                // 新SKU已存在于购物车中，合并数量
                int newQuantity = existingItem.getQuantity() + quantity;
                cartMapper.updateQuantity(existingItem.getCartItemId(), newQuantity, now);
                // 删除原购物车项
                cartMapper.deleteById(cartItemUpdateDTO.getCartItemId());
            } else {
                // 新SKU不存在于购物车中或就是当前项，直接更新SKU ID和数量
                cartMapper.updateSkuId(
                        cartItemUpdateDTO.getCartItemId(),
                        cartItemUpdateDTO.getSkuId(),
                        quantity,
                        now);
            }
        } else {
            // 只更新数量
            cartMapper.updateQuantity(
                    cartItemUpdateDTO.getCartItemId(),
                    cartItemUpdateDTO.getQuantity(),
                    now);
        }
    }

    @Override
    public void deleteCartItem(Integer cartItemId) {
        // 删除购物车项
        cartMapper.deleteById(cartItemId);
    }

    @Override
    public void clearCart() {
        // 从BaseContext中获取当前用户ID
        Integer userId = BaseContext.getCurrentId().intValue();
        // 删除当前用户的所有购物车项
        cartMapper.deleteByUserId(userId);
    }

    @Override
    public List<CartItem> getCartList() {
        // 从BaseContext中获取当前用户ID
        Long userIdLong = BaseContext.getCurrentId();
        log.info("获取购物车列表 - 当前用户ID(Long): {}", userIdLong);

        if (userIdLong == null) {
            log.error("获取购物车列表 - 用户ID为空");
            return new ArrayList<>();
        }

        Integer userId = userIdLong.intValue();
        log.info("获取购物车列表 - 当前用户ID(Integer): {}", userId);

        // 获取当前用户的购物车列表
        log.info("获取购物车列表 - 准备查询购物车，userId={}", userId);
        List<CartItem> cartItems = cartMapper.findByUserId(userId);
        log.info("获取购物车列表 - 查询结果: 共{}个商品", cartItems.size());

        // 为每个购物车项添加SKU详细信息
        for (CartItem cartItem : cartItems) {
            log.info("获取购物车列表 - 处理购物车项: cartItemId={}, skuId={}, quantity={}", cartItem.getCartItemId(),
                    cartItem.getSkuId(), cartItem.getQuantity());
            // 根据skuId查询SKU信息
            ProductSku sku = productSkuMapper.findById(cartItem.getSkuId());

            // 如果SKU存在，获取对应的商品名称
            if (sku != null) {
                // 根据productId查询商品信息，获取商品名称
                Product product = productMapper.findById(sku.getProductId());
                if (product != null) {
                    // 设置商品名称到SKU对象中
                    sku.setProductName(product.getProductName());
                }
            }

            if (sku != null && sku.getSkuImage() != null && !sku.getSkuImage().isBlank()) {
                sku.setSkuImage(aliyunOSSOperator.generateSignedUrl(sku.getSkuImage()));
            }

            // 设置SKU信息到购物车项
            cartItem.setSku(sku);
        }

        log.info("获取购物车列表 - 处理完成，返回{}个商品", cartItems.size());
        return cartItems;
    }
}
