FROM postgres
ENV POSTGRES_DB githubsync
ENV POSTGRES_USER githubsync
ENV POSTGRES_PASSWORD githubsync
COPY psql_schema.sql /docker-entrypoint-initdb.d/
