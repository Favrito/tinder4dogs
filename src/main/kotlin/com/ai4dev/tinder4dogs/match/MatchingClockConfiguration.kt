package com.ai4dev.tinder4dogs.match

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MatchingClockConfiguration {

    @Bean
    fun matchingClock(): Clock = Clock.systemUTC()
}
