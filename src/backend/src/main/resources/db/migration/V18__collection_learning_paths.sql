ALTER TABLE study_topics
    ADD COLUMN collection_id BIGINT REFERENCES collections(id) ON DELETE SET NULL,
    ADD COLUMN learning_area_type VARCHAR(30) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN refresh_status VARCHAR(40) NOT NULL DEFAULT 'CURRENT',
    ADD COLUMN current_version INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN source_signature VARCHAR(64),
    ADD COLUMN generation_failure VARCHAR(500),
    ADD COLUMN last_refreshed_at TIMESTAMPTZ;

ALTER TABLE study_topics
    ADD CONSTRAINT ck_study_topic_area_type
        CHECK (learning_area_type IN ('LEGACY', 'COLLECTION_BACKED')),
    ADD CONSTRAINT ck_study_topic_refresh_status
        CHECK (refresh_status IN ('NOT_BUILT', 'BUILDING', 'CURRENT', 'NEW_KNOWLEDGE_AVAILABLE', 'REFRESHING', 'FAILED')),
    ADD CONSTRAINT ck_study_topic_current_version CHECK (current_version >= 0);

CREATE UNIQUE INDEX uk_study_topics_active_collection
    ON study_topics(collection_id)
    WHERE collection_id IS NOT NULL AND status = 'ACTIVE';
CREATE INDEX ix_study_topics_collection
    ON study_topics(collection_id, updated_at DESC);

ALTER TABLE topic_concepts
    ADD COLUMN stable_key VARCHAR(240),
    ADD COLUMN lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE topic_concepts
    ADD CONSTRAINT ck_topic_concept_lifecycle
        CHECK (lifecycle_status IN ('ACTIVE', 'RETIRED'));

CREATE INDEX ix_topic_concepts_topic_lifecycle
    ON topic_concepts(topic_id, lifecycle_status, position);

CREATE TABLE learning_path_versions (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES study_topics(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    source_signature VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,
    CONSTRAINT uk_learning_path_topic_version UNIQUE(topic_id, version_number),
    CONSTRAINT ck_learning_path_version_number CHECK(version_number > 0),
    CONSTRAINT ck_learning_path_status CHECK(status IN ('CURRENT', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_learning_path_current_topic
    ON learning_path_versions(topic_id)
    WHERE status = 'CURRENT';
CREATE INDEX ix_learning_path_topic_created
    ON learning_path_versions(topic_id, created_at DESC);

CREATE TABLE learning_modules (
    id BIGSERIAL PRIMARY KEY,
    path_version_id BIGINT NOT NULL REFERENCES learning_path_versions(id) ON DELETE CASCADE,
    stage VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL,
    title VARCHAR(240) NOT NULL,
    objective TEXT NOT NULL,
    CONSTRAINT uk_learning_module_version_position UNIQUE(path_version_id, position),
    CONSTRAINT ck_learning_module_stage CHECK(stage IN ('FOUNDATION', 'CORE', 'APPLICATION', 'ADVANCED')),
    CONSTRAINT ck_learning_module_position CHECK(position > 0)
);

CREATE INDEX ix_learning_modules_version_stage
    ON learning_modules(path_version_id, stage, position);

CREATE TABLE learning_module_resources (
    module_id BIGINT NOT NULL REFERENCES learning_modules(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    source_role VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(module_id, resource_id, source_role),
    CONSTRAINT ck_learning_module_source_role CHECK(source_role IN ('PRIMARY', 'SUPPORTING'))
);

CREATE INDEX ix_learning_module_resources_resource
    ON learning_module_resources(resource_id, module_id);

CREATE TABLE learning_module_concepts (
    path_version_id BIGINT NOT NULL REFERENCES learning_path_versions(id) ON DELETE CASCADE,
    module_id BIGINT NOT NULL REFERENCES learning_modules(id) ON DELETE CASCADE,
    concept_id BIGINT NOT NULL REFERENCES topic_concepts(id) ON DELETE RESTRICT,
    position INTEGER NOT NULL,
    PRIMARY KEY(module_id, concept_id),
    CONSTRAINT uk_learning_path_concept UNIQUE(path_version_id, concept_id),
    CONSTRAINT uk_learning_module_concept_position UNIQUE(module_id, position),
    CONSTRAINT ck_learning_module_concept_position CHECK(position > 0)
);

CREATE INDEX ix_learning_module_concepts_concept
    ON learning_module_concepts(concept_id, path_version_id);

CREATE OR REPLACE FUNCTION mark_collection_learning_area_stale()
RETURNS TRIGGER AS $$
DECLARE
    affected_collection BIGINT;
    affected_resource BIGINT;
    resource_ready BOOLEAN;
BEGIN
    IF TG_OP = 'DELETE' THEN
        affected_collection := OLD.collection_id;
        affected_resource := OLD.resource_id;
    ELSE
        affected_collection := NEW.collection_id;
        affected_resource := NEW.resource_id;
    END IF;
    SELECT processing_status = 'READY' INTO resource_ready
    FROM resources WHERE id = affected_resource;

    IF TG_OP = 'DELETE' OR COALESCE(resource_ready, FALSE) THEN
        UPDATE study_topics
        SET refresh_status = 'NEW_KNOWLEDGE_AVAILABLE', updated_at = now()
        WHERE collection_id = affected_collection
          AND learning_area_type = 'COLLECTION_BACKED'
          AND status = 'ACTIVE'
          AND current_version > 0;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_collection_learning_area_membership
AFTER INSERT OR DELETE ON resource_collections
FOR EACH ROW EXECUTE FUNCTION mark_collection_learning_area_stale();

CREATE OR REPLACE FUNCTION mark_ready_resource_learning_areas_stale()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.processing_status = 'READY' AND OLD.processing_status <> 'READY' THEN
        UPDATE study_topics st
        SET refresh_status = 'NEW_KNOWLEDGE_AVAILABLE', updated_at = now()
        FROM resource_collections rc
        WHERE rc.resource_id = NEW.id
          AND st.collection_id = rc.collection_id
          AND st.learning_area_type = 'COLLECTION_BACKED'
          AND st.status = 'ACTIVE'
          AND st.current_version > 0;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ready_resource_learning_areas
AFTER UPDATE OF processing_status ON resources
FOR EACH ROW EXECUTE FUNCTION mark_ready_resource_learning_areas_stale();

CREATE OR REPLACE FUNCTION sync_learning_area_collection_name()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE study_topics
    SET title = NEW.name, updated_at = now()
    WHERE collection_id = NEW.id AND learning_area_type = 'COLLECTION_BACKED';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_learning_area_collection_name
AFTER UPDATE OF name ON collections
FOR EACH ROW EXECUTE FUNCTION sync_learning_area_collection_name();

CREATE OR REPLACE FUNCTION archive_deleted_collection_learning_area()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE study_topics
    SET learning_area_type = 'LEGACY', status = 'ARCHIVED', refresh_status = 'CURRENT', updated_at = now()
    WHERE collection_id = OLD.id AND learning_area_type = 'COLLECTION_BACKED';
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_archive_deleted_collection_learning_area
BEFORE DELETE ON collections
FOR EACH ROW EXECUTE FUNCTION archive_deleted_collection_learning_area();
