package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;

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
}
