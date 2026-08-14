package com.groupsync.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.groupsync.backend.knowledge.rag.GeminiProperties;

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties.class)
public class GroupSyncBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GroupSyncBackendApplication.class, args);
	}

}
