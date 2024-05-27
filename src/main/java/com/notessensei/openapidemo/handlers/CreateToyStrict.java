package com.notessensei.openapidemo.handlers;

import com.notessensei.openapidemo.Storage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.validation.ValidatedRequest;

public class CreateToyStrict implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext ctx) {
        ValidatedRequest request = ctx.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
        JsonObject body = request.getBody().getJsonObject();
        Storage.INSTANCE.addToy(body);
        ctx.json(body);
    }
}
