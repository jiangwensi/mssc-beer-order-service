package guru.sfg.beer.brewery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by Jiang Wensi on 9/11/2020
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateOrderRequest implements Serializable {
    private static final long serialVersionUID = -7807527142442919860L;
    private BeerOrderDto beerOrderDto;
}
