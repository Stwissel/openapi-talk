/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.notessensei.openapidemo.handlers.EchoHandler;
import io.quarkus.runtime.util.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.validation.ValidatorErrorType;
import io.vertx.openapi.validation.ValidatorException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This class represents a router for handling HTTP requests in the OpenAPI demo application.
 * It extends the AbstractVerticle class and is responsible for starting and stopping the server,
 * defining router actions, and setting up routes.
 */
@ApplicationScoped
public class MyRouter extends AbstractVerticle {

    static final String OPENAPI = "/webroot/openapidemo.json";
    static final String DEFAULT_CSP_VALUE = "default-src 'self'; img-src 'self' data:;";
    static final String HEADER_CSP = "Content-Security-Policy";

    static Handler<RoutingContext> createContentSecurityHeaderHandler(
            final String cspValue) {
        return ctx -> {
            final String realValue =
                    StringUtil.isNullOrEmpty(cspValue) ? DEFAULT_CSP_VALUE : cspValue;
            final HttpServerResponse response = ctx.response();
            if (response.headers().contains(HEADER_CSP)) {
                response.headers().remove(HEADER_CSP);
            }
            response.putHeader(HEADER_CSP, realValue);
            ctx.next();
        };
    }


    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        System.out.println("MyRouter up and away");
        this.bringupTheServer(startPromise);
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        System.out.println("Gone with the wind!");
        stopPromise.complete();
    }

    /**
     * Starts the server and sets up the router actions.
     *
     * @param promise
     */
    void bringupTheServer(Promise<Void> promise) {

        HttpServer server = vertx.createHttpServer();
        JsonObject spec = OPENAPI.endsWith(".yaml")
                ? Utils.jsonFromYamlResource(OPENAPI)
                : Utils.jsonFromResource(OPENAPI);

        OpenAPIContract.from(this.getVertx(), spec)
                .compose(this::defineRouterActions)
                .compose(this::manualRoutes)
                .compose(this::addCspHandler)
                .compose(router -> server.requestHandler(router).listen(8080))
                .onSuccess(r -> {
                    System.out.printf("%nServer up and running on port %s%n%n", r.actualPort());
                    promise.complete();
                })
                .onFailure(promise::fail);
    }

    Future<Router> addCspHandler(final Router router) {

        // Ensure csp comes first
        Handler<RoutingContext> cspHandler = createContentSecurityHeaderHandler(null);
        router.route().order(-1).handler(cspHandler);
        return Future.succeededFuture(router);
    }

    Future<Router> defineRouterActions(final OpenAPIContract contract) {
        final RouterBuilder builder = RouterBuilder.create(this.getVertx(), contract);

        // All the security schemes and handlers
        final Set<String> schemes = new HashSet<>();
        contract.getSecurityRequirements().stream()
                .flatMap(sr -> sr.getNames().stream())
                .forEach(scheme -> {
                    System.out.println("Scheme: " + scheme);
                    schemes.add(scheme);
                });

        schemes.forEach(scheme -> {
            Optional<BasicAuthHandler> handler = Utils.getAuthenticationHandler(scheme);
            handler.ifPresent(h -> {
                System.out.println("Adding security handler for " + scheme);
                builder.security(scheme).httpHandler(h);
            });
        });


        // individual actions
        builder.getRoutes().forEach(this::setupRoute);

        // further processing
        return Future.succeededFuture(builder.createRouter());
    }

    Future<Router> manualRoutes(final Router router) {

        /* Create the handler for the Swagger UI */
        String source = "META-INF/resources/webjars/swagger-ui/5.17.11";


        Handler<RoutingContext> initHandler = ctx -> {
            ctx.response().putHeader("content-type", "application/javascript; charset=UTF8");
            vertx.fileSystem().readFile("webroot/swagger-initializer.js")
                    .onSuccess(buffer -> ctx.response().end(buffer))
                    .onFailure(ctx::fail);
        };
        router.route("/openapi/swagger-initializer.js").handler(initHandler);

        StaticHandler swaggerUI = StaticHandler.create(FileSystemAccess.RELATIVE, source)
                .setFilesReadOnly(true)
                .setIndexPage("index.html")
                .setDefaultContentEncoding("UTF-8");

        router.route("/openapi/*").handler(swaggerUI);


        /* Static Router catch all, must be last */
        StaticHandler root = StaticHandler.create()
                .setFilesReadOnly(true)
                .setIndexPage("index.html")
                .setDefaultContentEncoding("UTF-8");

        router.route().order(999).handler(root);

        return Future.succeededFuture(router);
    }

    /**
     * Sets up a route based on the provided OpenAPIRoute.
     *
     * @param route The OpenAPIRoute object containing the route information.
     */
    void setupRoute(final OpenAPIRoute route) {
        Operation operation = route.getOperation();
        System.out.println("Found Operation " + operation.getOperationId());
        Handler<RoutingContext> handler =
                Utils.getRouteHandler(operation.getOperationId()).orElse(new EchoHandler());
        route.addHandler(handler);
        route.addFailureHandler(this::youScrewedUp);
    }

    /**
     * Handles the case when something goes wrong in the routing process.
     * If there is a failure, it sets the appropriate status code and response message.
     * If there is no failure, it sets the status code to 500 and returns a generic error message.
     *
     * @param ctx the routing context
     */
    void youScrewedUp(RoutingContext ctx) {
        Throwable f = ctx.failure();
        ctx.response().putHeader("content-type", "application/json; charset=UTF8");

        JsonObject response = new JsonObject()
                .put("message", f.getMessage())
                .put("code", ctx.statusCode());

        if (f instanceof ValidatorException) {
            ValidatorException ve = (ValidatorException) f;
            ValidatorErrorType vet = ve.type();
            response.put("type", vet.toString());
        } else if (f instanceof HttpException) {
            HttpException he = (HttpException) f;
            response.put("payload", he.getPayload());
        }
        ctx.response()
                .setStatusCode(ctx.statusCode())
                .end(response.toBuffer());
    }
}
