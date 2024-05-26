/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.notessensei.openapidemo.handlers.EchoHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.validation.ValidatorException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This class represents a router for handling HTTP requests in the OpenAPI demo application.
 * It extends the AbstractVerticle class and is responsible for starting and stopping the server,
 * defining router actions, and setting up routes.
 */
@ApplicationScoped
public class MyRouter extends AbstractVerticle {

    static final String OPENAPI = "/openapidemo.json";

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
                .compose(router -> server.requestHandler(router).listen(8080))
                .onSuccess(r -> {
                    System.out.printf("%nServer up and running on port %s%n%n", r.actualPort());
                    promise.complete();
                })
                .onFailure(promise::fail);
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
        router.route("/").handler(ctx -> ctx.response().end("Hello World"));
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
        if (f == null) {
            ctx.response()
                    .setStatusCode(500)
                    .end("{\"code\": 500, \"message\": \"Unspecified screwup -> we are really sorry \"}");
            return;
        }
        int screwup = f instanceof ValidatorException ? 400 : ctx.statusCode();
        JsonObject response = new JsonObject()
                .put("code", screwup)
                .put("message", f.getMessage());
        ctx.response().setStatusCode(screwup)
                .end(response.toBuffer());
    }
}
