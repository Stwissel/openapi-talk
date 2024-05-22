# openapi-talk

This is the code repository for the talk [OpenAPI - whe what, wyh and how](https://stwissel.github.io/presentations/OpenAPI2023/index.html) ([Repo](https://github.com/Stwissel/presentations))

This project uses Quarkus, the Supersonic Subatomic Java Framework. If you want to learn more about Quarkus, please visit [its website](https://quarkus.io/).

## Prerequisites

- current Java (tested with 17)
- Apache maven installed (and on the path)
- Postman when you want to use the postman collection

## Running the application in dev mode

That's what you want since it allows hot reload (a.k.a live coding):

```bash
mvn compile quarkus:dev
```

You now can interact with the application on port `8080`

- `/` -> Hello world (no Demo without Hello World)
- `http://localhost:8080/q/dev/` -> Quarkus dev UI
- any route defined in openapidemo.yaml

## Packaging and running

Follow the Quarkus [documentation](https://quarkus.io/)

The application can be packaged and run as `quarkus-run.jar` using:

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

To build an _über-jar_ execute the following command:

```bash
mvn package -Dquarkus.package.type=uber-jar
java -jar target/*-runner.jar
```

There are options for containers or native executables described in the Quarkus documentation.
If you want to learn more, [please consult](https://quarkus.io/guides/maven-tooling).

## Postman

In `src/main/postman` there's a postman collection you can use to interact with the API to learn about its behaviors
