package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.brewery.model.events.ValidateOrderRequest;
import guru.sfg.beer.brewery.model.events.ValidateOrderResult;
import guru.sfg.beer.order.service.config.JMSConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.security.MessageDigestSpi;

/**
 * Created by Jiang Wensi on 10/11/2020
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;
    @JmsListener(destination = JMSConfig.VALIDATE_ORDER_QUEUE)
    public void listen(Message msg){
        boolean isValid = true;
        ValidateOrderRequest request = (ValidateOrderRequest)msg.getPayload();

        if(request.getBeerOrderDto().getCustomerRef()!=null && request.getBeerOrderDto().getCustomerRef().equals(
                "fail-validation")){
            isValid=false;
        }

        jmsTemplate.convertAndSend(
                JMSConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateOrderResult
                        .builder()
                        .isValid(isValid)
                        .orderId(request.getBeerOrderDto().getId())
                        .build());
    }
}
