package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.JsonNode;

class RestResponse {
    private Object content;
    private JsonNode controls;

    public RestResponse() {}

    public RestResponse(Object content, JsonNode controls) {
        this.content = content;
        this.controls = controls;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public JsonNode getControls() {
        return controls;
    }

    public void setControls(JsonNode controls) {
        this.controls = controls;
    }
}
