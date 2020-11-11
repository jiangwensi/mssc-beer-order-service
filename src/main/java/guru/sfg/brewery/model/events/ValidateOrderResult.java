package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Jiang Wensi on 9/11/2020
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateOrderResult implements Serializable {
    private static final long serialVersionUID = 7880689715608373677L;
    private UUID orderId;
    private boolean isValid;
}
