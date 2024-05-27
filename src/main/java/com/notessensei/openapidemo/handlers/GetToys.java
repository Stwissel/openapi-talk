package com.notessensei.openapidemo.handlers;

import com.notessensei.openapidemo.Storage;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;

public class GetToys implements Handler<RoutingContext> {
    public void handle(RoutingContext ctx) {
        ValidatedRequest request = ctx.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
        RequestParameter qeueryColor = request.getQuery().get("color");
        JsonArray reply = qeueryColor == null
                ? Storage.INSTANCE.getToys()
                : Storage.INSTANCE.getToys(qeueryColor.getString());
        ctx.response()
                .putHeader("content-type", "application/json")
                .end(reply.encode());
    }
}
