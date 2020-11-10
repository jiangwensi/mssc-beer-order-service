package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.brewery.model.events.ValidateOrderRequest;
import guru.sfg.beer.order.service.config.JMSConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by Jiang Wensi on 9/11/2020
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {

        UUID orderId = UUID.fromString((String)context.getMessageHeader(BeerOrderManagerImpl.ORDER_ID_HEADER));
        BeerOrder beerOrder = beerOrderRepository.findOneById(orderId);

        jmsTemplate.convertAndSend(
                JMSConfig.VALIDATE_ORDER_QUEUE,
                ValidateOrderRequest
                    .builder()
                    .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                    .build());
        System.out.println("Sending validate order request for order id "+orderId);
    }
}
