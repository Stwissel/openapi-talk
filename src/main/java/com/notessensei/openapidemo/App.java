/* (C) 2023 notessensei, Apache-2.0 license */

package com.notessensei.openapidemo;

import java.util.ArrayList;
import java.util.List;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
/**
 * The main application class for initializing and running the application.
 */
public class App {

    /**
     * Initializes the application on startup.
     *
     * @param e The startup event
     * @param vertx The Vert.x instance
     * @param verticles The collection of verticles to deploy
     */
    public void init(@Observes StartupEvent e, Vertx vertx, Instance<AbstractVerticle> verticles) {

        System.out.println(e.getClass().getName());

        List<Future<?>> loadedVerticles = new ArrayList<>();
        for (AbstractVerticle verticle : verticles) {
            loadedVerticles.add(vertx.deployVerticle(verticle));
        }
        Future.all(loadedVerticles)
                .onSuccess(v -> System.out.println("All up and running"))
                .onFailure(System.err::println);
    }
}
