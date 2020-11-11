package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.events.AllocationFailureEvent;
import guru.sfg.brewery.model.events.DeallocateOrderRequest;
import guru.sfg.beer.order.service.config.JMSConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Created by Jiang Wensi on 10/11/2020
 */
@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIT {

    public static final String CUSTOMER_REF_VALIDATION_FAILED = "customer-ref-validation-failed";
    public static final String CUSTOMER_REF_VALIDATION_PENDING = "customer-ref-validation-pending";
    public final static String CUSTOMER_REF_ALLOCATION_PENDING_INVENTORY = "customer-ref-allocation-pending-inventory";
    public final static String CUSTOMER_REF_ALLOCATION_ERROR = "customer-ref-allocation-error";
    public final static String CUSTOMER_REF_ALLOCATION_PENDING = "customer-ref-allocation-pending";

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BeerOrderMapper beerOrderMapper;

    @Autowired
    JmsTemplate jmsTemplate;

    Customer testCustomer;

    UUID beerId;

    BeerOrder beerOrder;

    @TestConfiguration
    static class RestTemplateBuilderProvider {
        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
        beerId = UUID.randomUUID();

        wireMockServer
                .stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                        .willReturn(
                                okJson(
                                        objectMapper.writeValueAsString(
                                                BeerDto.builder()
                                                        .id(beerId)
                                                        .upc("12345")
                                                        .build()))));
        beerOrder = createBeerOrder();
    }

    @Test
    void testNewToAllocated() {

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        beerOrderWaitUntil(BeerOrderStatusEnum.ALLOCATED);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(savedBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testFailedValidation() {
        beerOrder.setCustomerRef(CUSTOMER_REF_VALIDATION_FAILED);
        beerOrderManager.newBeerOrder(beerOrder);
        beerOrderWaitUntil(BeerOrderStatusEnum.VALIDATION_EXCEPTION);
    }

    @Test
    void testAllocationFailure() {
        beerOrder.setCustomerRef(CUSTOMER_REF_ALLOCATION_ERROR);
        beerOrderManager.newBeerOrder(beerOrder);

        beerOrderWaitUntil(BeerOrderStatusEnum.ALLOCATION_EXCEPTION);

        AllocationFailureEvent allocationFailureEvent = (AllocationFailureEvent)
                jmsTemplate.receiveAndConvert(JMSConfig.ALLOCATE_FAILURE_QUEUE);
        assertEquals(beerOrder.getId().toString(), allocationFailureEvent.getBeerOrderId());
    }

    @Test
    void testPartialAllocation() {
        beerOrder.setCustomerRef(CUSTOMER_REF_ALLOCATION_PENDING_INVENTORY);
        beerOrderManager.newBeerOrder(beerOrder);
        beerOrderWaitUntil(BeerOrderStatusEnum.PENDING_INVENTORY);
    }

    @Test
    void testNewToPickUp() {

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        beerOrderWaitUntil(BeerOrderStatusEnum.ALLOCATED);

        Optional<BeerOrder> savedBeerOrderOptional = beerOrderRepository.findById(savedBeerOrder.getId());
        assertTrue(savedBeerOrderOptional.isPresent());

        beerOrderManager.beerOrderPickedUp(savedBeerOrderOptional.get().getId());

        beerOrderWaitUntil(BeerOrderStatusEnum.PICKED_UP);
        Optional<BeerOrder> pickedBeerOrderOptional = beerOrderRepository.findById(beerOrder.getId());
        assertTrue(pickedBeerOrderOptional.isPresent());
        assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedBeerOrderOptional.get().getOrderStatus());
    }

    @Test
    void testValidationPendingToCancel() {
        beerOrder.setCustomerRef(CUSTOMER_REF_VALIDATION_PENDING);
        beerOrderManager.newBeerOrder(beerOrder);

        beerOrderWaitUntil(BeerOrderStatusEnum.VALIDATION_PENDING);

        beerOrderManager.cancelBeerOrder(beerOrder.getId());
        beerOrderWaitUntil(BeerOrderStatusEnum.CANCELED);
        Optional<BeerOrder> pickedBeerOrderOptional = beerOrderRepository.findById(beerOrder.getId());
        assertTrue(pickedBeerOrderOptional.isPresent());
        assertEquals(BeerOrderStatusEnum.CANCELED, pickedBeerOrderOptional.get().getOrderStatus());
    }

    @Test
    void testAllocationPendingToCancel() {
        beerOrder.setCustomerRef(CUSTOMER_REF_ALLOCATION_PENDING);
        beerOrderManager.newBeerOrder(beerOrder);

        beerOrderWaitUntil(BeerOrderStatusEnum.ALLOCATION_PENDING);

        beerOrderManager.cancelBeerOrder(beerOrder.getId());

        beerOrderWaitUntil(BeerOrderStatusEnum.CANCELED);

        Optional<BeerOrder> pickedBeerOrderOptional = beerOrderRepository.findById(beerOrder.getId());
        assertTrue(pickedBeerOrderOptional.isPresent());
        assertEquals(BeerOrderStatusEnum.CANCELED, pickedBeerOrderOptional.get().getOrderStatus());

    }

    @Test
    void testAllocatedToCancel() {
        beerOrderManager.newBeerOrder(beerOrder);

        beerOrderWaitUntil(BeerOrderStatusEnum.ALLOCATED);

        beerOrderManager.cancelBeerOrder(beerOrder.getId());

        beerOrderWaitUntil(BeerOrderStatusEnum.CANCELED);

        DeallocateOrderRequest deallocateOrderRequest =
                (DeallocateOrderRequest)jmsTemplate.receiveAndConvert(JMSConfig.DEALLOCATE_ORDER_QUEUE);
        assertNotNull(deallocateOrderRequest);
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();

        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }

    private void beerOrderWaitUntil(BeerOrderStatusEnum status) {
        await().untilAsserted(() -> {
            assertEquals(status, beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus()
            );
        });
    }
}
