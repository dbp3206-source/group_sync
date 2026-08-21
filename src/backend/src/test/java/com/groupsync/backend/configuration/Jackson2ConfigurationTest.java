package com.groupsync.backend.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class Jackson2ConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(Jackson2Configuration.class);

    @Test
    void exposesSingleJackson2MapperForKnowledgeServices() {
        contextRunner.run(context -> {
            assertDoesNotThrow(() -> context.getBean(ObjectMapper.class));
            assertEquals(1, context.getBeansOfType(ObjectMapper.class).size());
            assertDoesNotThrow(() -> context.getBean(ObjectMapper.class)
                    .readValue("[\"verified\"]", List.class));
        });
    }
}
