package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.brewery.model.CustomerDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Created by Jiang Wensi on 11/11/2020
 */

@Mapper(uses = {DateMapper.class})
public interface CustomerMapper {
    CustomerDto customerToCustomerDto(Customer customer);
    Customer customerDtoToCustomer(CustomerDto customerDto);
}
