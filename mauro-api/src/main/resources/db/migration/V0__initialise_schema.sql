-- # Core

-- Authority
CREATE TABLE core."authority" (
    "id"                              UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "version"                         INTEGER          NOT NULL,
    "date_created"                    TIMESTAMP,
    "last_updated"                    TIMESTAMP,
    "readable_by_everyone"            BOOLEAN          NOT NULL,
    "readable_by_authenticated_users" BOOLEAN          NOT NULL,
    "label"                           TEXT             NOT NULL,
    "description"                     TEXT,
    "aliases_string"                  TEXT,
    "created_by"                      VARCHAR(255),
    "breadcrumb_tree_id"              UUID,
    "url"                             VARCHAR(255)     NOT NULL
);

-- Folder
CREATE TABLE core."folder" (
    "id"                              UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "version"                         INTEGER          NOT NULL,
    "date_created"                    TIMESTAMP,
    "last_updated"                    TIMESTAMP,
    "readable_by_everyone"            BOOLEAN          NOT NULL,
    "readable_by_authenticated_users" BOOLEAN          NOT NULL,
    "label"                           TEXT             NOT NULL,
    "description"                     TEXT,
    "created_by"                      VARCHAR(255),
    "finalised"                       BOOLEAN          NOT NULL,
    "date_finalised"                  TIMESTAMP,
    "documentation_version"           VARCHAR(255),
    "deleted"                         BOOLEAN          NOT NULL,
    "authority_id"                    UUID /*NOT NULL REFERENCES authority(id)*/,
    "branch_name"                     VARCHAR(255),
    "model_version"                   VARCHAR(255),
    "model_version_tag"               VARCHAR(255),
    "parent_folder_id"                UUID,
    "class"                           VARCHAR(255)
);
CREATE INDEX "idx_folder_parent_folder_id" ON core."folder"(parent_folder_id);



-- # Terminology

CREATE TABLE terminology."terminology" (
    "id"                              UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "version"                         INTEGER          NOT NULL,
    "date_created"                    TIMESTAMP,
    "last_updated"                    TIMESTAMP,
    "readable_by_everyone"            BOOLEAN          NOT NULL,
    "readable_by_authenticated_users" BOOLEAN          NOT NULL,
    "label"                           VARCHAR(255)     NOT NULL,
    "description"                     VARCHAR(255),
    "aliases_string"                  VARCHAR(255),
    "created_by"                      VARCHAR(255),
    "breadcrumb_tree_id"              UUID,
    "finalised"                       BOOLEAN          NOT NULL,
    "date_finalised"                  TIMESTAMP,
    "documentation_version"           VARCHAR(255),
    "model_type"                      VARCHAR(255)     NOT NULL,
    "organisation"                    VARCHAR(255),
    "deleted"                         BOOLEAN          NOT NULL,
    "author"                          VARCHAR(255),
    "folder_id"                       UUID             NOT NULL REFERENCES core.folder(id),
    "authority_id"                    UUID /*NOT NULL REFERENCES authority(id)*/,
    "branch_name"                     VARCHAR(255),
    "model_version"                   VARCHAR(255),
    "model_version_tag"               VARCHAR(255)
);
CREATE UNIQUE INDEX "idx_terminology_folder_id_label_branch_name_model_version" ON terminology."terminology"(folder_id, label, branch_name, model_version);

CREATE TABLE terminology."term_relationship_type" (
    "id"                    UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "version"               INTEGER          NOT NULL,
    "date_created"          TIMESTAMP,
    "last_updated"          TIMESTAMP,
    "label"                 TEXT             NOT NULL,
    "description"           TEXT,
    "aliases_string"        VARCHAR(255),
    "created_by"            VARCHAR(255),
    "breadcrumb_tree_id"    UUID,
    "idx"                   INTEGER,
    "terminology_id"        UUID             NOT NULL REFERENCES terminology.terminology(id),
    "parental_relationship" BOOLEAN,
    "child_relationship"    BOOLEAN
);
CREATE UNIQUE INDEX "idx_term_relationship_type_terminology_id_label" ON terminology."term_relationship_type"(terminology_id, label);


CREATE TABLE terminology."term" (
    "id"                 UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "version"            INTEGER          NOT NULL,
    "date_created"       TIMESTAMP,
    "last_updated"       TIMESTAMP,
    "label"              TEXT             NOT NULL,
    "description"        TEXT,
    "aliases_string"     VARCHAR(255),
    "created_by"         VARCHAR(255),
    "breadcrumb_tree_id" UUID,
    "idx"                INTEGER,
    "terminology_id"     UUID             NOT NULL REFERENCES terminology.terminology(id),
    "code"               VARCHAR(255)     NOT NULL,
    "definition"         TEXT             NOT NULL,
    "url"                VARCHAR(255),
    "is_parent"          BOOLEAN,
    "depth"              INTEGER
);
CREATE UNIQUE INDEX "idx_term_terminology_id_code" ON terminology."term"(terminology_id, code);

CREATE TABLE terminology."term_relationship" (
    "id"                   UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "version"              INTEGER          NOT NULL,
    "date_created"         TIMESTAMP,
    "last_updated"         TIMESTAMP,
    "label"                TEXT             NOT NULL,
    "description"          TEXT,
    "aliases_string"       VARCHAR(255),
    "created_by"           VARCHAR(255),
    "breadcrumb_tree_id"   UUID,
    "idx"                  INTEGER,
    "terminology_id"       UUID             NOT NULL REFERENCES terminology.terminology(id),
    "source_term_id"       UUID             NOT NULL REFERENCES terminology.term(id),
    "target_term_id"       UUID             NOT NULL REFERENCES terminology.term(id),
    "relationship_type_id" UUID             NOT NULL REFERENCES terminology.term_relationship_type(id)
);
CREATE INDEX "idx_term_relationship_terminology_id" ON terminology."term_relationship"(terminology_id);
CREATE INDEX "idx_term_relationship_source_term_id" ON terminology."term_relationship"(source_term_id);
CREATE INDEX "idx_term_relationship_target_term_id" ON terminology."term_relationship"(target_term_id);
CREATE INDEX "idx_term_relationship_relationship_type_id" ON terminology."term_relationship"(relationship_type_id);

-- Test tables
CREATE TABLE "test" ("id" UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),"version" INTEGER NOT NULL,"date_created" TIMESTAMP,"last_updated" TIMESTAMP,"readable_by_everyone" BOOLEAN NOT NULL,"readable_by_authenticated_users" BOOLEAN NOT NULL,"label" VARCHAR(255) NOT NULL,"description" VARCHAR(255),"aliases_string" VARCHAR(255),"created_by" VARCHAR(255),"breadcrumb_tree_id" UUID,"finalised" BOOLEAN NOT NULL,"date_finalised" TIMESTAMP,"documentation_version" VARCHAR(255),"model_type" VARCHAR(255) NOT NULL,"organisation" VARCHAR(255),"deleted" BOOLEAN NOT NULL,"author" VARCHAR(255),"folder_id" UUID NOT NULL,"authority_id" UUID,"branch_name" VARCHAR(255),"model_version" VARCHAR(255),"model_version_tag" VARCHAR(255));
CREATE TABLE "test_term" ("test_id" UUID NOT NULL,"term_id" UUID NOT NULL);
