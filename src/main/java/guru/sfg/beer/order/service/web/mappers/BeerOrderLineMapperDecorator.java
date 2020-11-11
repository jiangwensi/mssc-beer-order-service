package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * Created by Jiang Wensi on 6/11/2020
 */
public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {
    @Autowired
    private BeerService beerService;
    @Autowired
    private BeerOrderLineMapper beerOrderLineMapper;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine beerOrderLine){
        BeerOrderLineDto orderLineDto = beerOrderLineMapper.beerOrderLineToDto(beerOrderLine);
        Optional<BeerDto> beerDtoOptional = beerService.getBeerDtoByUpc(orderLineDto.getUpc());
        beerDtoOptional.ifPresent(beerDto -> {
            orderLineDto.setBeerName(beerDto.getBeerName());
            orderLineDto.setBeerStyle(beerDto.getBeerStyle());
            orderLineDto.setPrice(beerDto.getPrice());
            orderLineDto.setBeerId(beerDto.getId());
        });
        return orderLineDto;
    }
}
