package io.vertx.webpro.example.dto;

import lombok.Data;

@Data
public class Query {

    private String requestId;
    private int page;
    private int size;
}
