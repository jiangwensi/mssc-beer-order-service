package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.brewery.model.events.ValidateOrderResult;
import guru.sfg.beer.order.service.config.JMSConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by Jiang Wensi on 10/11/2020
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ValidationResultListener {
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JMSConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateOrderResult result){
        System.out.println("listen JMSConfig.VALIDATE_ORDER_RESPONSE_QUEUE");
        UUID id = result.getOrderId();
        System.out.println("Validation result for order id: "+id);
        beerOrderManager.processValidationResult(id,result.isValid());
    }
}
