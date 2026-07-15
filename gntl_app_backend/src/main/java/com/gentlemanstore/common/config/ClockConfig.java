package com.gentlemanstore.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Clock kao bean — servisi koji zavise od "sada" (mesečni izveštaji) postaju
 * testabilni sa Clock.fixed(), bez oslanjanja na stvarni datum.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
