package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.beer.brewery.model.BeerDto;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    public static final String VALIDATION_FAILED = "validation-failed";
    public final static String ALLOCATION_PENDING_INVENTORY = "allocation-pending-inventory";
    public final static String ALLOCATION_ERROR = "allocation-error";

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

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus().equals(BeerOrderStatusEnum.ALLOCATED));

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
        beerOrder.setCustomerRef(VALIDATION_FAILED);
        beerOrderManager.newBeerOrder(beerOrder);
        Awaitility.setDefaultTimeout(10,TimeUnit.SECONDS);
        await().untilAsserted(() -> {
            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION,
                    beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus()
            );
        });
    }

    @Test
    void testAllocationFailure() {
        beerOrder.setCustomerRef(ALLOCATION_ERROR);
        beerOrderManager.newBeerOrder(beerOrder);
        await().untilAsserted(() -> {
            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION,
                    beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus()
            );
        });
    }

    @Test
    void testPartialAllocation() {
        beerOrder.setCustomerRef(ALLOCATION_PENDING_INVENTORY);
        beerOrderManager.newBeerOrder(beerOrder);
        await().untilAsserted(() -> {
            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY,
                    beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus()
            );
        });
    }


    @Test
    void testNewToPickUp() {

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        Awaitility.setDefaultTimeout(10,TimeUnit.SECONDS);
        await().untilAsserted(() -> {
            assertEquals(BeerOrderStatusEnum.ALLOCATED,
                    beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus()
            );
        });
        Optional<BeerOrder> savedBeerOrderOptional = beerOrderRepository.findById(savedBeerOrder.getId());
        assertTrue(savedBeerOrderOptional.isPresent());

        beerOrderManager.beerOrderPickedUp(savedBeerOrderOptional.get().getId());

        await().untilAsserted(() -> {
            assertEquals(BeerOrderStatusEnum.PICKED_UP,
                    beerOrderRepository.findById(beerOrder.getId()).get().getOrderStatus()
            );
        });
        Optional<BeerOrder> pickedBeerOrderOptional = beerOrderRepository.findById(beerOrder.getId());
        assertTrue(pickedBeerOrderOptional.isPresent());
        assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedBeerOrderOptional.get().getOrderStatus());
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
}
