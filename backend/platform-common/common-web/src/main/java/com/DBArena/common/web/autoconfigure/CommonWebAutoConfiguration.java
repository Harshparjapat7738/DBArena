package com.DBArena.common.web.autoconfigure;

import com.DBArena.common.web.GlobalProblemExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link GlobalProblemExceptionHandler} for any Spring Boot
 * service that depends on common-web - no per-service {@code @Import} or
 * component-scan change needed. Mirrors common-security's
 * {@code CommonSecurityAutoConfiguration} pattern exactly.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalProblemExceptionHandler.class)
    public GlobalProblemExceptionHandler globalProblemExceptionHandler() {
        return new GlobalProblemExceptionHandler();
    }
}
