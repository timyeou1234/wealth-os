# Wealth OS API

The API is an independently deployable Kotlin application that will evolve into the
Spring Boot modular monolith described in the architecture documentation.

The current skeleton is deliberately framework-free. It provides only the Kotlin/JVM
build needed for domain-model work.

## Requirements

- JDK 21
- No globally installed Gradle

## Commands

Run from the repository root:

```bash
./gradlew :apps:api:test
./gradlew :apps:api:build
```

## IntelliJ IDEA

Open the repository root as a Gradle project and configure:

- Gradle distribution: Wrapper
- Gradle JVM: Java 21
- Build and test using: Gradle

The root package is `com.wealthos`. Spring Boot, persistence, HTTP, and business-domain
types are intentionally absent from this bootstrap.
