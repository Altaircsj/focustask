package br.edu.ufersa.pw.focustask.features.focusSession;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class FocusSessionConfiguration {
    @Bean
    Clock focusSessionClock() {
        return Clock.systemUTC();
    }
}
