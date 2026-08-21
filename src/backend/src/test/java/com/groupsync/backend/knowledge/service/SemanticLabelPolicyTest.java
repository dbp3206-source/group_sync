package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticLabelPolicyTest {
    @Test void ragAndLongFormShareCanonicalKey() { assertEquals(SemanticLabelPolicy.equivalenceKey("RAG"), SemanticLabelPolicy.equivalenceKey("Retrieval-Augmented Generation")); }
    @Test void oopAndLongFormShareCanonicalKey() { assertEquals(SemanticLabelPolicy.equivalenceKey("OOP"), SemanticLabelPolicy.equivalenceKey("Object Oriented Programming")); }
    @Test void postgresAndPostgresqlShareCanonicalKey() { assertEquals(SemanticLabelPolicy.equivalenceKey("Postgres"), SemanticLabelPolicy.equivalenceKey("PostgreSQL")); }
    @Test void ragAndSemanticSearchAreNotEquivalent() { assertNotEquals(SemanticLabelPolicy.equivalenceKey("RAG"), SemanticLabelPolicy.equivalenceKey("Semantic Search")); }
    @Test void ragAndCardiologyAreNotEquivalent() { assertNotEquals(SemanticLabelPolicy.equivalenceKey("RAG"), SemanticLabelPolicy.equivalenceKey("Cardiology")); }
    @Test void vagueTagsAreRejected() { assertEquals(List.of("SQL"), SemanticLabelPolicy.usefulTags(List.of("document", "chapter", "important", "pdf", "SQL"))); }
    @Test void duplicateCandidatesAreRemoved() { assertEquals(1, SemanticLabelPolicy.usefulTags(List.of("RAG", "Retrieval Augmented Generation", "rag")).size()); }
    @Test void tagCountIsBounded() { assertEquals(6, SemanticLabelPolicy.usefulTags(List.of("A1","A2","A3","A4","A5","A6","A7","A8")).size()); }
}
