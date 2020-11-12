package guru.sfg.beer.order.service.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by Jiang Wensi on 12/11/2020
 */
@Profile("localdiscovery")
@EnableDiscoveryClient
@Configuration
public class LocalDiscovery {
}
