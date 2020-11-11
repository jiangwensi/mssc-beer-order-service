package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.brewery.model.events.AllocationFailureEvent;
import guru.sfg.beer.order.service.config.JMSConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Created by Jiang Wensi on 11/11/2020
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        String beerOrderId = (String) context.getMessageHeader(BeerOrderManagerImpl.ORDER_ID_HEADER);

        jmsTemplate.convertAndSend(
                JMSConfig.ALLOCATE_FAILURE_QUEUE,
                AllocationFailureEvent
                        .builder()
                        .beerOrderId(beerOrderId)
                        .build());

        log.debug("Sent allocation failure message to queue for order id "+beerOrderId);
    }
}
