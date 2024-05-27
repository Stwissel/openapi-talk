package com.notessensei.openapidemo.handlers;

import com.notessensei.openapidemo.Storage;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class GetToy implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext ctx) {
        String[] x = ctx.request().path().split("/");
        String toyId = x[x.length - 1];

        Storage.INSTANCE.getToy(toyId)
                .ifPresentOrElse(
                        toy -> ctx.json(toy),
                        () -> ctx.response()
                                .setStatusCode(404)
                                .end("Toy not found"));
    }

}
