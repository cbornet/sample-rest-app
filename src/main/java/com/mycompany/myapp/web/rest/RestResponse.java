package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestResponse<T> {
    private T content;
    private JsonNode controls;

    public RestResponse() {}

    public RestResponse(T content, JsonNode controls) {
        this.content = content;
        this.controls = controls;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public JsonNode getControls() {
        return controls;
    }

    public void setControls(JsonNode controls) {
        this.controls = controls;
    }

    public static <T> ResponseEntity<RestResponse<T>> wrapResponse(ResponseEntity<T> response, JsonNode controls) {
        final RestResponse<T> restResponse = new RestResponse<>(response.getBody(), controls);

        return ResponseEntity
            .status(response.getStatusCode() != HttpStatus.NO_CONTENT ? response.getStatusCode() : HttpStatus.OK)
            .headers(response.getHeaders())
            .body(restResponse);
    }
}
