package com.groupsync.backend.knowledge.ingestion;

import java.util.*;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.shared.exception.BadRequestException;
@Component public class ResourceParserRegistry { private final Map<ResourceType, ResourceParser> parsers; public ResourceParserRegistry(List<ResourceParser> parsers) { Map<ResourceType, ResourceParser> map = new EnumMap<>(ResourceType.class); parsers.forEach(parser -> map.put(parser.supports(), parser)); this.parsers = Map.copyOf(map); } public ResourceParser forType(ResourceType type) { ResourceParser parser = parsers.get(type); if (parser == null) throw new BadRequestException("No parser is configured for this resource type."); return parser; } }
