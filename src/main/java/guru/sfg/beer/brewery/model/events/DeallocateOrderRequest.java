package guru.sfg.beer.brewery.model.events;

import guru.sfg.beer.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by Jiang Wensi on 11/11/2020
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeallocateOrderRequest implements Serializable {
    private static final long serialVersionUID = -3533431164672663583L;
    private BeerOrderDto beerOrderDto;
}
