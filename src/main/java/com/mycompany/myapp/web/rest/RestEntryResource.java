package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RestController
public class RestEntryResource {
    private final TemplateEngine templateEngine;
    private final ObjectMapper mapper;

    public RestEntryResource(TemplateEngine templateEngine, ObjectMapper mapper) {
        this.templateEngine = templateEngine;
        this.mapper = mapper;
    }

    @GetMapping("/api")
    public RestResponse getEntities() throws IOException {
        String content = templateEngine.process("oai/entities.json", new Context());
        return new RestResponse(null, mapper.readTree(content));
    }
}
