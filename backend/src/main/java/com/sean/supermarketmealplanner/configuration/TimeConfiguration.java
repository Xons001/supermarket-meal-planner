package com.sean.supermarketmealplanner.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean Clock applicationClock() { return Clock.systemUTC(); }
}
