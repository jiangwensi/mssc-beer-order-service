package guru.sfg.beer.order.service.services;

import guru.sfg.beer.brewery.model.BeerOrderDto;
import guru.sfg.beer.brewery.model.events.ValidateOrderResult;
import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidationResult(UUID beerOrderId, Boolean isValid);

    void processAllocationResult(BeerOrderDto beerOrderDto, Boolean allocationError, Boolean pendingInventory);

    void beerOrderPickedUp(UUID beerOrderId);
}
