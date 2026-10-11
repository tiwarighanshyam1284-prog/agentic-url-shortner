# Link Shortener

A URL shortener written in Java 17 with Spring Boot. It has a REST API, short links that redirect, click statistics, custom names, expiry and disabling.

## What it does

| You do | The service does |
|---|---|
| Send a long web address | Returns a short address such as `http://localhost:8080/AbC1234` |
| Open the short address | Sends you on to the long address (HTTP 302) and counts the click |
| Ask for statistics | Returns the total number of clicks and the time of the last click |
| Disable a link | Stops it from redirecting (HTTP 410 from then on) |

## Test page

When the application is running, open **http://localhost:8080/urlshortner** in a browser. The page lets you create links, open them (which counts a click), show statistics and disable links. It calls the same REST API as everything else.

## REST API

| Method | Path | Purpose | Success | Common errors |
|---|---|---|---|---|
| GET | `/api/v1/health` | Health check | 200 | |
| POST | `/api/v1/links` | Create a link. Body: `url` (required), `alias` (optional), `ttlSeconds` (optional) | 201 + `Location` | 400 invalid input, 409 alias in use |
| GET | `/api/v1/links/{code}` | Details of a link | 200 | 404 |
| GET | `/api/v1/links/{code}/stats` | Click statistics | 200 | 404 |
| DELETE | `/api/v1/links/{code}` | Disable a link (repeat calls are harmless) | 204 | 404 |
| GET | `/{code}` | Follow a short link | 302 + `Location` | 404 unknown, 410 disabled or expired |

Errors all have the same shape: `{"error": "Bad Request", "message": "...", "status": 400}`.

Example:

```bash
curl -i -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -d '{"url": "https://docs.oracle.com/en/java/", "alias": "my-docs"}'

curl -i http://localhost:8080/my-docs              # 302 to the Oracle page
curl http://localhost:8080/api/v1/links/my-docs/stats
```

On Windows PowerShell, use `curl.exe` instead of `curl`, or use the requests in `http/requests.http`.

## Run it in IntelliJ IDEA

1. Install a JDK 17 or newer (JDK 21 is fine).
2. **File > Open**, select the `pom.xml` file in this folder, and choose **Open as Project**.
3. Wait for Maven to finish importing. The first time, it downloads Spring Boot. This needs internet access.
4. Open `src/main/java/com/linkshortener/LinkShortenerApplication.java` and click the green arrow beside `main`.
5. The Run window shows the Spring Boot banner and `Tomcat started on port 8080`.
6. Open `http/requests.http` and click **Run** beside each request, in order. This runs the demo without a terminal.
7. To stop, click the red square.

To run the tests: open the **Maven** tool window, expand **Lifecycle**, and double-click **test**. Expect 0 failures.

## Run it from a terminal

```bash
mvn spring-boot:run                 # Maven 3.9+ and JDK 17+ required
mvn test                            # runs the tests
mvn -q -DskipTests package && java -jar target/link-shortener-1.0.0.jar
```

## Settings

Defaults are in `src/main/resources/application.yml`.

| Setting | Environment variable | Default |
|---|---|---|
| Port | `SERVER_PORT` | `8080` |
| Address used in short links | `SHORTENER_BASE_URL` | `http://localhost:8080` |

## Project structure

```
link-shortener/
├── pom.xml                                   Maven build (Spring Boot 3.3.4, Java 17)
├── README.md                                 this file
├── http/requests.http                        demo requests for IntelliJ's HTTP client
└── src/
    ├── main/
    │   ├── resources/application.yml         settings
    │   ├── resources/static/urlshortner.html       test page at http://localhost:8080/urlshortner
    │   └── java/com/linkshortener/
    │       ├── LinkShortenerApplication.java start-up
    │       ├── api/                          REST layer (HTTP only, no rules)
    │       │   ├── LinkController.java       the routes
    │       │   ├── ApiExceptionHandler.java  errors become JSON replies
    │       │   └── dto/                      request and reply shapes
    │       ├── service/                      business rules (no HTTP)
    │       │   ├── LinkService.java          create, follow, count, disable
    │       │   ├── UrlValidator.java         input rules
    │       │   ├── CodeGenerator.java        random short codes
    │       │   └── ApiException.java         an error with an HTTP status
    │       ├── model/ShortLink.java          one link and its click counter
    │       ├── repository/                   where links are kept
    │       │   ├── LinkRepository.java       interface
    │       │   └── InMemoryLinkRepository.java
    │       └── config/                       settings and shared beans
    └── test/java/com/linkshortener/
        ├── service/                          rule tests (no Spring needed)
        └── api/LinkApiTest.java              HTTP tests through the full application
```

The dependency direction is one way: `api` calls `service`, and `service` calls `model` and `repository`. The service never imports HTTP types except `HttpStatus` in `ApiException`.

## Design decisions

- **Layers.** Controllers only translate HTTP to calls and back. Rules live in `LinkService`, so they can be tested without starting a server.
- **Interface for storage.** `LinkRepository` is an interface. The in-memory version can be replaced by a database without changing the rules.
- **Thread safety without locks.** Links are stored with `ConcurrentHashMap.putIfAbsent`, so two requests can never take the same alias. Clicks use `AtomicLong`.
- **Expiry uses a clock.** `LinkService` reads time from a `Clock` bean, so tests can move time forward instead of waiting.
- **Clicks are counted only for links that redirect.** Expired and disabled links are refused and not counted.
- **Internal addresses are refused.** `localhost`, private network addresses and single-word host names are rejected, so the shortener cannot be used to reach internal services.
- **Random codes.** Codes are 7 characters from `SecureRandom`. Generating one is retried up to 5 times if it is already taken.

## Known limits

- Links and click counts are kept in memory. They are lost when the application stops. A database would fix this; see the repository interface.
- There is no login. Anyone who can reach the port can create or disable links. Put the service behind authentication before using it on a shared network.
- The service listens on all network addresses, as Spring Boot does by default.
- There is no per-client rate limit yet. A burst of requests is accepted.
