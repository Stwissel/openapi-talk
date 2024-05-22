/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo.security;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;

public class Richelieu implements BasicAuthHandler {
    @Override
    public void handle(RoutingContext ctx) {
        MultiMap headers = ctx.request().headers();
        if (!headers.contains("Richelieu")) {
            ctx.fail(401, new Exception("You are not the Cardinal"));
        } else {
            ctx.next();
        }
    }
}
