package com.notessensei.openapidemo;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class MyRouterTest {

    static String verticleId = null;

    @BeforeAll
    static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MyRouter())
                .onFailure(testContext::failNow)
                .onSuccess(id -> {
                    verticleId = id;
                    testContext.completeNow();
                });
    }

    @AfterAll
    static void undeploy_verticle(Vertx vertx, VertxTestContext testContext) {
        if (verticleId != null) {
            vertx.undeploy(verticleId)
                    .onFailure(testContext::failNow)
                    .onSuccess(v -> testContext.completeNow());
        } else {
            testContext.completeNow();
        }
    }

    @Test
    @Order(1)
    void load_ball() {

        JsonObject toy = new JsonObject()
                .put("Name", "ball")
                .put("Color", "blue")
                .put("Shape", "round");

        given()
                .body(toy.encode())
                .contentType("application/json")
                .when()
                .post("/actual/toys")
                .then()
                .statusCode(200)
                .body("Color", equalTo("blue"));
    }

    @Test
    @Order(2)
    void fetch_alltoys() {
        when()
                .get("/toys")
                .then()
                .statusCode(200)
                .body("[0].Color", equalTo("blue"));
    }

    @Test
    @Order(3)
    void fetch_ball() {
        when()
                .get("/toys/ball")
                .then()
                .statusCode(200)
                .body("Name", equalTo("ball"))
                .body("Color", equalTo("blue"))
                .body("Shape", equalTo("round"));
    }
}
