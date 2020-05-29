package io.vertx.webpro.example;

import io.vertx.webpro.core.HttpServerPro;
import io.vertx.core.Vertx;

public class TestMain {

    static {
        System.setProperty(
                "vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.SLF4JLogDelegateFactory"
        );
    }

    public static void main(String[] args) {
        HttpServerPro.run(Vertx.vertx(), TestMain.class);
    }
}
