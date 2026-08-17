package com.groupsync.backend.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import com.groupsync.backend.knowledge.model.ResourceType;
public interface ResourceParser { ResourceType supports(); ParsedResourceContent parse(InputStream input) throws IOException; }
