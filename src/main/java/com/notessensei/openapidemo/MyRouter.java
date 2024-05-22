/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo;

import java.util.List;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.SecurityRequirement;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
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
        JsonObject spec = Utils.jsonFromResource(OPENAPI);

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

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // All the security schemes and handlers
        final List<SecurityRequirement> allRequirements = contract.getSecurityRequirements();
        allRequirements.forEach(req -> {
            System.out.println("Security Requirement: " + req);
            req.getNames().forEach(scheme -> {
                System.out.println("Scheme: " + scheme);
                Utils.getAuthenticationHandler(scheme, classLoader)
                        .ifPresentOrElse(handler -> builder.security(scheme).httpHandler(handler),
                                () -> {
                                    System.out.println("No handler for " + scheme);
                                    // Throw something here
                                });
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

    void setupRoute(final OpenAPIRoute route) {
        Operation operation = route.getOperation();
        System.out.println("Found Operation " + operation.getOperationId());
        route.addHandler(this::echoHandler);
        route.addFailureHandler(this::youScrewedUp);
    }

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

    void echoHandler(RoutingContext ctx) {
        ValidatedRequest request = ctx.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
        RequestParameter body = request.getBody();
        JsonObject reply = new JsonObject();
        JsonObject query = new JsonObject();
        JsonObject headers = new JsonObject();
        JsonObject pathparam = new JsonObject();
        reply.put("pathparam", pathparam)
                .put("headers", headers)
                .put("query", query)
                .put("body", body.get());
        request.getQuery().entrySet()
                .forEach(entry -> query.put(entry.getKey(), entry.getValue()));
        request.getHeaders().entrySet()
                .forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        ctx.response()
                .putHeader("content-type", "application/json")
                .end(reply.encode());
    }

}
