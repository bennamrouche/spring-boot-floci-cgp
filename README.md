# Spring Boot + Floci Cloud Storage

A small file manager that demonstrates Google Cloud Storage integration with Spring Boot. The application uses [Floci GCP](https://floci.io/gcp/) as a local Cloud Storage emulator, so you can try the upload, browse, download, and delete flow without connecting to a Google Cloud project. The app supplies a local-only placeholder access token for Floci; do not use that token with Google Cloud.

## Features

- Upload files from a web page
- Browse stored files with file size and type information
- Download or delete stored files
- Create the configured bucket automatically at application startup
- Persist emulator data between Floci restarts
- Use the Google Cloud Storage Java client through Spring Cloud GCP

## Technology

- Java 21
- Spring Boot 4
- Spring Cloud GCP Storage
- Maven
- Floci GCP emulator in Docker Compose
- Thymeleaf and Lucide icons

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- Docker Engine with the Docker Compose plugin

Check your installed tools:

```bash
java -version
mvn -version
docker compose version
```

## Run the application

1. Start Floci:

   ```bash
   docker compose up -d
   ```

2. Start the Spring Boot app in another terminal:

   ```bash
   mvn spring-boot:run
   ```

3. Open [http://localhost:8080](http://localhost:8080), select a file, and upload it. Use the Download or Delete action on a file card to retrieve or remove it.

The app connects to Floci at `http://localhost:4588` and creates the `uploads` bucket on startup if it does not exist. The Compose service uses the `floci/floci-gcp:latest` image and persists emulator state in the project’s `./data` directory.

## Build and run the JAR

Package the application without running tests (this project does not currently include a test suite):

```bash
mvn -DskipTests package
```

Then run it:

```bash
java -jar target/spring-boot-floci-gcs-0.0.1-SNAPSHOT.jar
```

Floci must be running before the application starts.

## Configuration

The local defaults are defined in `src/main/resources/application.yml`:

| Setting | Default | Environment variable |
| --- | --- | --- |
| Floci Storage endpoint | `http://localhost:4588` | `STORAGE_EMULATOR_HOST` |
| Cloud Storage bucket | `uploads` | `GCS_BUCKET` |
| Application port | `8080` | `SERVER_PORT` or Spring property `server.port` |
| Maximum uploaded file size | `100MB` | `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` |
| Maximum upload request size | `100MB` | `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` |

For example, to use a different endpoint or bucket:

```bash
STORAGE_EMULATOR_HOST=http://localhost:4588 GCS_BUCKET=my-uploads mvn spring-boot:run
```

You can change the upload limits in `src/main/resources/application.yml` or with the environment variables above.

## Emulator data and cleanup

Compose mounts the local `./data` directory into Floci, so buckets and uploaded objects remain available across container restarts.

Stop Floci and keep the data:

```bash
docker compose down
```

To start it again:

```bash
docker compose up -d
```

To reset all local emulator data, stop Floci and remove the `data` directory. This permanently removes all persisted buckets and objects in the local emulator:

```bash
docker compose down
rm -rf data
```

## Troubleshooting

- **The browser cannot connect:** make sure the Spring app is running and open `http://localhost:8080` (not a `file://` URL).
- **The app cannot reach Cloud Storage:** check that Floci is running with `docker compose ps` and that port `4588` is available.
- **Uploads fail because of size:** the default upload limit is 100 MB; adjust both multipart size settings in `application.yml`.
- **Icons do not appear:** the page loads Lucide from a CDN, so the browser needs internet access for the icons. The rest of the page remains usable without them.

## Project layout

```text
.
├── docker-compose.yml
├── pom.xml
└── src/main
    ├── java/net/alphaben/floci/storage
    │   ├── FileController.java
    │   ├── FlociStorageApplication.java
    │   └── StorageConfiguration.java
    └── resources
        ├── application.yml
        └── templates/index.html
```
