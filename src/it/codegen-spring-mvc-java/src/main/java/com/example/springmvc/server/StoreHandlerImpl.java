package com.example.springmvc.server;

import com.example.springmvc.server.petstore.definitions.Order;
import com.example.springmvc.server.petstore.store.StoreHandler;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletionStage;

@Service
public class StoreHandlerImpl implements StoreHandler {
    @Override
    public CompletionStage<GetInventoryResponse> getInventory() {
        return null;
    }

    @Override
    public CompletionStage<PlaceOrderResponse> placeOrder(Order body) {
        return null;
    }

    @Override
    public CompletionStage<GetOrderByIdResponse> getOrderById(long orderId) {
        return null;
    }

    @Override
    public CompletionStage<DeleteOrderResponse> deleteOrder(long orderId) {
        return null;
    }
}
