package com.rakshi.service;

import com.rakshi.domains.Order;
import com.rakshi.dtos.OrderDTO;
import com.rakshi.dtos.OrderItemDTO;

import java.util.List;

public interface OrderService {

    OrderDTO createOrder(OrderDTO order);
//    Order getOrder(int orderId);
//    Order updateOrder(Order order);
//    void deleteOrder(int orderId);
//    Order findOrderById(int orderId);
//    List<Order> findAllOrders();
//    List<Order> findAllOrdersByCustomer(int customerId);
}
