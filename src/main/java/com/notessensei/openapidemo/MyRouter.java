/* (C) 2023 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
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
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.openapi.validation.ValidatorException;
import jakarta.enterprise.context.ApplicationScoped;

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

        /* Hard coded to John Doe and password */
        BasicAuthHandler johnDoeHandler = new BasicAuthHandler() {

            @Override
            public void handle(RoutingContext ctx) {
                MultiMap headers = ctx.request().headers();
                String auth = headers.get("Authorization");
                // John Doe:password = Sm9obiBEb2U6cGFzc3dvcmQ=
                if (!"Basic Sm9obiBEb2U6cGFzc3dvcmQ=".equals(auth)) {
                    ctx.fail(401, new Exception("Boiling the TAR, go away"));
                } else {
                    ctx.next();
                }
            }
        };

        BasicAuthHandler richelieuHandler = new BasicAuthHandler() {

            @Override
            public void handle(RoutingContext ctx) {
                MultiMap headers = ctx.request().headers();
                if (!headers.contains("Richelieu")) {
                    ctx.fail(401, new Exception("You are not the Cardinal"));
                } else {
                    ctx.next();
                }
            }
        };

        // Security Schemes
        builder.security("UserPassword").httpHandler(johnDoeHandler);
        builder.security("Richelieu").httpHandler(richelieuHandler);

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
