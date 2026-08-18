# greeting-service

A deliberately small Spring Boot service wrapped in a full production-shaped build: PostgreSQL with
versioned migrations, a real HTTP API with validation and authorisation, container images from
buildpacks, and a CI pipeline that enforces formatting, coverage and static analysis.

The domain is one entity on purpose. The point of this repository is the machinery around it.

## Stack

|      Concern       |                       Choice                        |
|--------------------|-----------------------------------------------------|
| Language / runtime | Java 21                                             |
| Framework          | Spring Boot 3.5 (WebMVC, not WebFlux)               |
| Build              | Maven, pinned to 3.9.11 through the Maven wrapper   |
| Persistence        | Spring Data JPA / Hibernate on PostgreSQL 17        |
| Migrations         | Flyway, plain SQL, `ddl-auto: validate`             |
| Security           | Spring Security, HTTP Basic, stateless              |
| API docs           | springdoc-openapi, Swagger UI at `/swagger-ui.html` |
| Operations         | Actuator, health probes, Prometheus metrics         |
| Formatting         | Spotless with google-java-format, enforced in CI    |
| Coverage           | JaCoCo, 80% line / 70% branch, enforced in CI       |
| Static analysis    | SonarQube Cloud + GitHub CodeQL                     |
| Tests              | JUnit 5, Mockito, AssertJ, Testcontainers           |
| Container image    | Paketo buildpacks, no Dockerfile                    |
| CI/CD              | GitHub Actions, images published to GHCR            |

## Quick start

The only prerequisites are a JDK 21 and a running Docker daemon.

```bash
# Boot the app against a throwaway PostgreSQL that Testcontainers manages for you.
./mvnw spring-boot:test-run
```

That starts the service on <http://localhost:8080> with a real database and no setup. Then:

```bash
# Reads are public.
curl http://localhost:8080/api/v1/greetings

# Writes need an account.
curl -u user:password -X POST http://localhost:8080/api/v1/greetings \
  -H 'Content-Type: application/json' \
  -d '{"recipient":"Ada","language":"de"}'
# -> 201 Created, {"id":1,"recipient":"Ada","language":"de","message":"Hallo, Ada!", ...}
```

Swagger UI: <http://localhost:8080/swagger-ui.html>.

### Running against Docker Compose instead

```bash
./mvnw spring-boot:build-image -DskipTests   # produces ghcr.io/pgatzka/test:0.1.0-SNAPSHOT
docker compose up
```

Or bring up only the database and run the app from your IDE:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

## The API

Base path `/api/v1/greetings`. Errors are RFC 9457 problem details.

|  Method  |  Path   |       Auth        |                        Behaviour                        |
|----------|---------|-------------------|---------------------------------------------------------|
| `POST`   | `/`     | any account       | Renders a greeting, stores it, returns 201 + `Location` |
| `GET`    | `/{id}` | public            | One greeting, or 404                                    |
| `GET`    | `/`     | public            | Paged list, `?language=de&page=0&size=20`               |
| `DELETE` | `/{id}` | `ROLE_ADMIN` only | 204, or 404                                             |

Supported languages are `en`, `de`, `fr`, `es`. An omitted language defaults to English; an unknown
one is a 400 naming the codes that would have worked.

Two in-memory accounts exist for the demo: `user` / `password` and `admin` / `password`. They are
defined in `application.yml` under `app.security` and overridable by environment variable
(`APP_SECURITY_ADMIN_PASSWORD` and friends). **Replace them before running this anywhere real** — a
production deployment should authenticate against a real identity provider instead.

## Build commands

```bash
./mvnw test                 # unit + web-slice tests only, no Docker needed
./mvnw verify               # the full gate: format, unit, integration, coverage threshold
./mvnw spotless:apply       # fix formatting (run this before pushing)
./mvnw sonar:sonar          # analysis, after a verify; needs SONAR_TOKEN
```

`verify` is the command CI runs, and it is the one that matters. In order it runs Spotless's check,
the unit tests, the Testcontainers integration tests, then merges both coverage runs and fails if
the merged result is under threshold.

### Test layout

| Suffix  |  Plugin  | Needs Docker |                       What it covers                       |
|---------|----------|--------------|------------------------------------------------------------|
| `*Test` | Surefire | no           | Service logic, entity semantics, DTO validation, web slice |
| `*IT`   | Failsafe | yes          | Full stack: Tomcat, Spring Security, Flyway, PostgreSQL    |

Integration tests extend `AbstractPostgresIT`, which supplies a shared PostgreSQL container through
`@ServiceConnection` — no JDBC URL is configured anywhere in test properties. `OperationsIT` also
asserts that both Flyway migrations applied and that the V2 `CHECK` constraint really rejects a
blank recipient, so the migrations are tested rather than merely executed.

## Quality gates

**Spotless** formats Java with google-java-format, sorts imports, strips unused ones, applies a
license header, and sorts `pom.xml`. It is bound to `verify`, so unformatted code fails the build.
`./mvnw spotless:apply` fixes everything it complains about.

**JaCoCo** runs a separate agent over the unit and the integration test runs, merges the two
execution files, and enforces the merged result:

```xml
<jacoco.line.coverage>0.80</jacoco.line.coverage>
<jacoco.branch.coverage>0.70</jacoco.branch.coverage>
```

`GreetingApplication` and everything in `config/` are excluded — they are wiring, and testing them
measures Spring rather than this code. The merged HTML report lands in
`target/site/jacoco-merged/index.html`.

**SonarQube Cloud** consumes the merged JaCoCo XML, so its coverage figure matches the local gate.

**CodeQL** runs GitHub's `security-and-quality` query suite on every push and pull request, plus a
weekly scheduled sweep to catch newly published queries against unchanged code.

## CI/CD

`.github/workflows/ci.yml` has two jobs.

**`build`** runs on every push to `main`, every tag, and every pull request. It checks out with full
history (Sonar needs blame data), runs `./mvnw verify`, publishes a JUnit summary, uploads coverage
and test reports as artifacts, then runs Sonar. The Sonar step is guarded by
`if: env.SONAR_TOKEN != ''`, so pull requests from forks — which cannot see repository secrets —
skip analysis instead of failing.

**`image`** runs only on pushes, never on pull requests, so nothing from an untrusted branch is ever
published. It builds the image with buildpacks and pushes to GHCR:

- `:<project version>` — always
- `:sha-<short sha>` — always, and this is what deployments should pin
- `:latest` — only for `v*` tags, never for a moving branch

Authentication uses the built-in `GITHUB_TOKEN` with `packages: write`, so no registry secret needs
to be created.

### Setup checklist

Everything below is required before the pipeline is fully green. The build and image jobs work out
of the box; only Sonar needs configuring.

1. **SonarQube Cloud.** Create the project at <https://sonarcloud.io>, then correct these
   properties in `pom.xml` if your keys differ from the guesses committed here:

   ```xml
   <sonar.organization>pgatzka</sonar.organization>
   <sonar.projectKey>pgatzka_test</sonar.projectKey>
   ```

   Add the token as a repository secret named `SONAR_TOKEN`
   (*Settings → Secrets and variables → Actions*). In SonarCloud, set the project's analysis method
   to **CI-based**, not Automatic Analysis — the two conflict and the build will fail if both run.

2. **GHCR visibility.** The first push creates a private package. Make it public, or grant read
   access, under *Packages → greeting-service → Package settings* if you want to pull it anonymously.

3. **CodeQL.** Enable *Settings → Code security → Code scanning* if the repository is private;
   public repositories need nothing.

4. **Branch protection.** Require the `Build, test and analyse` check on `main` so the gates cannot
   be bypassed.

## Project layout

```
src/main/java/io/github/pgatzka/greeting/
├── GreetingApplication.java
├── config/           # security, OpenAPI, JPA auditing — excluded from coverage
├── domain/           # Greeting entity + repository
├── service/          # Language enum, GreetingService, domain exceptions
└── web/              # controller, problem-detail handler, DTO records

src/main/resources/
├── application.yml
└── db/migration/     # Flyway: V1 creates the table, V2 adds a CHECK constraint

src/test/java/io/github/pgatzka/greeting/
├── domain/ service/ web/   # *Test — fast, no Docker
└── support/                # Testcontainers config, IT base class, test-run entry point
```

## Design notes

A few choices that are deliberate rather than accidental:

- **Flyway owns the schema, Hibernate validates it.** `ddl-auto: validate` means a drift between the
  entities and the migrations fails at startup rather than silently corrupting data.
- **`open-in-view` is off.** The default leaves a database session open for the whole request, which
  hides lazy-loading mistakes until they become production latency.
- **DTO records, not entities, on the wire.** `PagedResponse` exists because serialising Spring's
  `Page` leaks its internals into the API and its JSON shape changes between Spring Data versions.
- **Language templates live on the enum.** Adding a language is a one-line change that never touches
  the service.
- **Unit and integration coverage are merged, not summed.** Measuring them separately makes it
  possible to hit a threshold twice without either suite covering a given branch.

## Licence

MIT.
