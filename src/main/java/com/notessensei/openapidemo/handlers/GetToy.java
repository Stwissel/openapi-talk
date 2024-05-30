package com.notessensei.openapidemo.handlers;

import java.util.Map;
import com.notessensei.openapidemo.Storage;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;

public class GetToy implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {

        /* This is the troubled where toyId isn't coming back */
        /* Some debug printouts */
        Route ctxRoute = ctx.currentRoute();
        System.out.printf("Current Route %s %n", ctxRoute.getPath());
        System.out.println("Metadata:");
        ctxRoute.metadata()
                .forEach((k, v) -> System.out.printf("Key: %s, Value: %s %n", k, v));

        ValidatedRequest request = ctx.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
        Map<String, RequestParameter> pParams = request.getPathParameters();
        System.out.println("ValidatedRequest parameter count:" + pParams.size());
        pParams.forEach((k, v) -> System.out.println(k + " => " + v.getString()));
        RequestParameter toyId = pParams.get("toyId");

        Storage.INSTANCE.getToy(toyId.getString())
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.response()
                                .setStatusCode(404)
                                .end("Toy not found!"));
    }

}
