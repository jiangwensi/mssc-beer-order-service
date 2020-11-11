package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import guru.sfg.beer.order.service.config.JMSConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImplIT.*;

/**
 * Created by Jiang Wensi on 10/11/2020
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JMSConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message message) {
        Boolean pendingInventory = false;
        Boolean allocationError = false;
        Boolean pendingAllocation = false;

        AllocateOrderRequest request = (AllocateOrderRequest) message.getPayload();
        String customerRef = request.getBeerOrderDto().getCustomerRef();

        if (customerRef != null) {
            if (customerRef.equals(CUSTOMER_REF_ALLOCATION_PENDING_INVENTORY)) {
                pendingInventory = true;
            }
            if (customerRef.equals(CUSTOMER_REF_ALLOCATION_ERROR)) {
                allocationError = true;
            }
            if (customerRef.equals(CUSTOMER_REF_ALLOCATION_PENDING)) {
                pendingAllocation = true;
            }
        }

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
        });

        if(!pendingAllocation){
            jmsTemplate.convertAndSend(
                    JMSConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                    AllocateOrderResult.builder()
                            .beerOrderDto(request.getBeerOrderDto())
                            .pendingInventory(pendingInventory)
                            .allocationError(allocationError)
                            .build());
        }
    }
}
