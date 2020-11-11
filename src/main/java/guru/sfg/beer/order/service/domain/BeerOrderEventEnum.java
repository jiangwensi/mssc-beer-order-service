package guru.sfg.beer.order.service.domain;

/**
 * Created by Jiang Wensi on 9/11/2020
 */
public enum BeerOrderEventEnum {
    VALIDATE_ORDER,
    CANCEL_ORDER,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    ALLOCATE_ORDER,
    ALLOCATION_SUCCESS,
    ALLOCATION_NO_INVENTORY,
    ALLOCATION_FAILED,
    PICK_UP_ORDER
}
