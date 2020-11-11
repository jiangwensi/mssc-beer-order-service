package guru.sfg.beer.order.service.services;

import guru.sfg.beer.brewery.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Jiang Wensi on 9/11/2020
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateMachineInterceptor;
    private final EntityManager entityManager;


    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void beerOrderPickedUp(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse((beerOrder) -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.PICK_UP_ORDER);
        }, () -> log.error("Order Not Found. Id: " + beerOrderId));
    }

    @Override
    public void cancelBeerOrder(UUID beerOrderId) {
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse((beerOrder) -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
        }, () -> log.error("Order Not Found. Id: " + beerOrderId));
    }

    @Transactional
    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        entityManager.flush();
        beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
            if (isValid) {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
                Optional<BeerOrder> validatedOrderOptional = beerOrderRepository.findById(beerOrderId);
                validatedOrderOptional.ifPresentOrElse(validatedOrder -> {
                    sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
                }, () -> log.error("Order Not Found. Id: " + beerOrderId));
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }
        }, () -> log.error("Order Not Found. Id: " + beerOrderId));
    }

    @Transactional
    @Override
    public void processAllocationResult(BeerOrderDto beerOrderDto, Boolean allocationError, Boolean pendingInventory) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            if (allocationError) {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
            } else if (pendingInventory) {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
                updateAllocatedQty(beerOrderDto);
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
                updateAllocatedQty(beerOrderDto);
            }
        }, () -> log.error("Order not found: " + beerOrderDto.getId()));
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse((allocatedOrder) -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
                    }
                });
            });
            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("Order not found. ID: " + beerOrderDto.getId()));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
        Message msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();
        sm.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());
        sm.stop();
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(beerOrderStateMachineInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                });
        sm.start();
        return sm;
    }
}
