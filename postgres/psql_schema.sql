CREATE TABLE repositories (
    id            integer PRIMARY KEY,
    repository    jsonb
);

CREATE TABLE issues (
    id            integer PRIMARY KEY,
    issue         jsonb,
    created_at    TIMESTAMP,
    repository_id integer references repositories(id)
);

CREATE INDEX issues_created_at on issues (created_at DESC);

CREATE TABLE etags (
    uri           varchar(250) PRIMARY KEY,
    etag          varchar(32),
    weak          boolean
);
