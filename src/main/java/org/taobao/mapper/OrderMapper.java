package org.taobao.mapper;

import org.apache.ibatis.annotations.Param;
import org.taobao.dto.OrderQueryDTO;
import org.taobao.pojo.Orders;
import org.taobao.pojo.OrderItem;

import java.util.List;
import java.util.Map;

/**
 * 订单Mapper
 */
public interface OrderMapper {

    /**
     * 插入订单
     * 
     * @param orders 订单信息
     */
    void insertOrder(Orders orders);

    /**
     * 插入订单商品项
     * 
     * @param orderItem 订单商品项
     */
    void insertOrderItem(OrderItem orderItem);

    /**
     * 获取订单列表
     * 
     * @param orderQueryDTO 查询条件
     * @return 订单列表
     */
    List<Orders> getOrderList(OrderQueryDTO orderQueryDTO);

    /**
     * 获取订单总数
     * 
     * @param orderQueryDTO 查询条件
     * @return 订单总数
     */
    Integer getOrderCount(OrderQueryDTO orderQueryDTO);

    /**
     * 根据ID获取订单
     * 
     * @param orderId 订单ID
     * @return 订单详情
     */
    Orders getOrderById(Integer orderId);

    /**
     * 根据订单ID获取订单商品项
     * 
     * @param orderId 订单ID
     * @return 订单商品项列表
     */
    List<OrderItem> getOrderItemsByOrderId(Integer orderId);

    /**
     * 根据订单ID和店铺ID获取订单商品项
     * 
     * @param orderId 订单ID
     * @param shopId  店铺ID
     * @return 订单商品项列表
     */
    List<OrderItem> getOrderItemsByOrderIdAndShopId(@Param("orderId") Integer orderId, @Param("shopId") Integer shopId);

    /**
     * 更新订单状态
     * 
     * @param orderId 订单ID
     * @param status  订单状态
     */
    void updateOrderStatus(@Param("orderId") Integer orderId, @Param("status") String status);

    /**
     * 获取用户订单状态统计
     * 
     * @param userId 用户ID
     * @return 各状态订单数量（包含pending、paid、shipped、completed、cancelled字段）
     */
    Map<String, Object> getOrderStatusStatistics(Integer userId);
}
