package com.rakshi.service.impls;

import com.rakshi.domains.Order;
import com.rakshi.dtos.OrderDTO;
import com.rakshi.mappers.OrderItemMapper;
import com.rakshi.mappers.OrderMapper;
import com.rakshi.service.OrderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {

    @Inject
    private OrderMapper orderMapper;

    @Inject
    private OrderItemMapper orderItemMapper;

    @Override
    public OrderDTO createOrder(OrderDTO orderRequest) {

        Order orderDomain = orderMapper.toDomain(orderRequest);
        System.out.println(orderDomain);

        orderRequest.getItems().stream().;

        return null;
    }
}
