package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.beer.order.service.config.JMSConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

/**
 * Created by Jiang Wensi on 10/11/2020
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AllocationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JMSConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResult result) {
        beerOrderManager.processAllocationResult(result.getBeerOrderDto(),result.getAllocationError(),
                result.getPendingInventory());
    }
}
