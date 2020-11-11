package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by Jiang Wensi on 11/11/2020
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AllocationFailureEvent implements Serializable {
    private static final long serialVersionUID = -6667009651275930745L;
    private String beerOrderId;
}
