package com.groupsync.backend.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps the Jackson 2 services used by the knowledge domain available while
 * Spring Boot 4's web stack uses Jackson 3.
 */
@Configuration(proxyBeanMethods = false)
public class Jackson2Configuration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper knowledgeObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
