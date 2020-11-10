package guru.sfg.beer.order.service.services;

import guru.sfg.beer.brewery.model.events.ValidateOrderResult;
import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void validateBeerOrder(UUID beerOrderId, Boolean isValid);
}
