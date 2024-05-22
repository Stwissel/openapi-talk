/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;

/**
 * A handler class that handles the request and generates a JSON response.
 * Used in the OpenAPI demo application as default handler.
 */
public class EchoHandler implements Handler<RoutingContext> {

    /**
     * Handles the incoming request and generates a JSON response.
     *
     * @param ctx the routing context
     */
    @Override
    public void handle(RoutingContext ctx) {
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
