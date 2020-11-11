package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by Jiang Wensi on 10/11/2020
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocateOrderResult implements Serializable {

    private static final long serialVersionUID = 6043193932444591272L;
    private BeerOrderDto beerOrderDto;
    private Boolean allocationError;
    private Boolean pendingInventory;

}
