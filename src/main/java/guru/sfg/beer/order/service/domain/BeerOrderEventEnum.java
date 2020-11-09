package guru.sfg.beer.order.service.domain;

/**
 * Created by Jiang Wensi on 9/11/2020
 */
public enum BeerOrderEventEnum {
    VALIDATION_ORDER,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    ALLOCATION_SUCCESS,
    ALLOCATION_NO_INVENTORY,
    ALLOCATION_FAILED,
    BEERORDER_PICKED_UP
}
