/* (C) 2023, 2024 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo.security;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;

public class UserPassword implements BasicAuthHandler {
    @Override
    public void handle(RoutingContext ctx) {
        /* Hard coded to John Doe and password */
        MultiMap headers = ctx.request().headers();
        String auth = headers.get("Authorization");
        // John Doe:password = Sm9obiBEb2U6cGFzc3dvcmQ=
        if (!"Basic Sm9obiBEb2U6cGFzc3dvcmQ=".equals(auth)) {
            ctx.fail(401, new Exception("Boiling the TAR, go away"));
        } else {
            ctx.next();
        }
    }
}
