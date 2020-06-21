CREATE TABLE repositories (
    id            integer PRIMARY KEY,
    repository    jsonb
);

CREATE TABLE issues (
    id            integer PRIMARY KEY,
    issue         jsonb,
    repository_id integer references repositories(id)
);
