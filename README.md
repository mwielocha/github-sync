# Github Synchronizer

Synchronizes all Haskell repositories that have at least one `good first` issue with a local `Postgres` database.

Additionaly, for each repository, the app also synchronizes all `good first` issues.
`/challenge` endpoint returns all synchronized repositories with the latest `good first` issue.

`/challenge` endpoint supports pagination with `offset` and `limit` query parameters. By default it returns first page of 50 results.

Issues are synchronized only if they were modified since last run. This works based on stored `etag` header values from github api responses.

## Running

App requires a local running `Postgres` database with configured schema.
It's recommended to use the included `Dockerfile` inside of the `postgres` directory:

``` sh
cd postgres/
docker build -t githubsync-db .
docker run -d -p 5432:5432 githubsync-db
cd ..

```

To run the app:

``` sh
sbt run
```

By default the app will run against the public github api with ip-based rate limits.
You can provide a github auth token in order to speed up the synchronization process:

``` sh
export _JAVA_OPTIONS="-Dgithub.username=<user> -Dgithub.authtoken=<token>"
```

## Testing

There are two separate test suites: `test` and `it:test`.
Integration tests run agains github api and require internet connectivity.
To test the app:

``` sh
sbt test it:test
```

Enjoy.
