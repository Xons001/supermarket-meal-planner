package com.sean.supermarketmealplanner.configuration;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {
    @Bean TimedAspect timedAspect(MeterRegistry registry) { return new TimedAspect(registry); }
}
