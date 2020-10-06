package com.mycompany.myapp.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

public class OhmResponse<T> {
    private static final Logger log = LoggerFactory.getLogger(OhmResponse.class);

    private T content;
    private OpenAPI controls = new OpenAPI();

    public OhmResponse() {}

    public OhmResponse(T content, OpenAPI controls) {
        this.content = content;
        this.controls = controls;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public OpenAPI getControls() {
        return controls;
    }

    public void setControls(OpenAPI controls) {
        this.controls = controls;
    }

    public OhmResponse<T> addControl(ControlBuilder controlBuilder) {
        return addControl(controlBuilder.build());
    }

    public OhmResponse<T> addControl(Control control) {
        if (controls.getPaths() == null) {
            controls.paths(new Paths());
        }
        final Paths paths = controls.getPaths();
        String newPathName = control.getPath();
        paths.putIfAbsent(newPathName, new PathItem());
        final PathItem newPathItem = paths.get(newPathName);
        newPathItem.operation(control.getMethod(), control.getOperation());
        return this;
    }

    public OhmResponse<T> addPaginationControls(ControlBuilder controlBuilder, Page<?> page) {
        return addPaginationControls(controlBuilder.build(), page);
    }

    public OhmResponse<T> addPaginationControls(Control control, Page<?> page) {
        addControl(control);
        String summary = control.getOperation().getSummary();
        Operation op = new Operation();
        control
            .getOperation()
            .getParameters()
            .stream()
            .filter(parameter -> !Set.of("page", "size", "sort").contains(parameter.getName()))
            .forEach(op::addParametersItem);
        if (controls.getPaths() == null) {
            controls.paths(new Paths());
        }
        final Paths paths = controls.getPaths();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
            .fromUriString(control.getPath())
            .replaceQueryParam("size", page.getSize());

        page
            .getSort()
            .stream()
            .map(sort -> sort.getProperty() + "," + sort.getDirection())
            .forEach(sort -> uriComponentsBuilder.queryParam("sort", sort));

        if (page.getTotalPages() > 1) {
            paths.put(
                uriComponentsBuilder.replaceQueryParam("page", 0).toUriString(),
                new PathItem()
                .get(
                        new Operation()
                            .parameters(op.getParameters())
                            .summary(String.format("%s [First page (1/%s)]", summary, page.getTotalPages()))
                    )
            );
        }
        if (page.getNumber() > 0) {
            paths.put(
                uriComponentsBuilder.replaceQueryParam("page", page.getNumber() - 1).toUriString(),
                new PathItem().get(op.summary(String.format("%s [Previous page (%s/%s)]", summary, page.getNumber(), page.getTotalPages())))
            );
        }
        if (page.getNumber() < page.getTotalPages() - 1) {
            paths.put(
                uriComponentsBuilder.replaceQueryParam("page", page.getNumber() + 1).toUriString(),
                new PathItem()
                .get(
                        new Operation()
                            .parameters(op.getParameters())
                            .summary(String.format("%s [Next page (%s/%s)]", summary, page.getNumber() + 2, page.getTotalPages()))
                    )
            );
        }
        if (page.getTotalPages() > 1) {
            paths.put(
                uriComponentsBuilder.replaceQueryParam("page", page.getTotalPages() - 1).toUriString(),
                new PathItem()
                .get(
                        new Operation()
                            .parameters(op.getParameters())
                            .summary(String.format("%s [Last page (%s/%s)]", summary, page.getTotalPages(), page.getTotalPages()))
                    )
            );
        }
        return this;
    }

    public static OhmResponse<Void> noContent() {
        return new OhmResponse<>(null, new OpenAPI());
    }

    public static <T> OhmResponse<T> of(T content) {
        return new OhmResponse<>(content, new OpenAPI());
    }

    public static <T> OhmResponse<T> of(ResponseEntity<T> content) {
        return new OhmResponse<>(content.getBody(), new OpenAPI());
    }

    public static <T> ResponseEntity<OhmResponse<T>> wrapResponse(ResponseEntity<T> response, OpenAPI controls) {
        final OhmResponse<T> restResponse = new OhmResponse<>(response.getBody(), controls);

        return ResponseEntity
            .status(response.getStatusCode() != HttpStatus.NO_CONTENT ? response.getStatusCode() : HttpStatus.OK)
            .headers(response.getHeaders())
            .body(restResponse);
    }

    public static ControlBuilder control(OpenAPI openAPI, PathItem.HttpMethod method, String path) {
        return new ControlBuilder(openAPI, method, path);
    }

    public static class ControlBuilder {
        private final OpenAPI openAPI;
        private final PathItem.HttpMethod method;
        private final String path;
        private final Operation op = new Operation();
        private Map<String, Object> parameters = new HashMap<>();

        public ControlBuilder(OpenAPI openAPI, PathItem.HttpMethod method, String path) {
            this.openAPI = openAPI;
            this.method = method;
            this.path = path;
        }

        public ControlBuilder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public ControlBuilder parameter(String name, Object value) {
            this.parameters.put(name, value);
            return this;
        }

        public ControlBuilder requestBody(RequestBody requestBody) {
            this.op.setRequestBody(requestBody);
            return this;
        }

        public ControlBuilder summary(String summary) {
            this.op.setSummary(summary);
            return this;
        }

        public Control build() {
            final PathItem pathItem = openAPI.getPaths().get(path);
            final Operation operation;
            final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(path);

            if (pathItem == null) {
                log.warn("Path " + path + "not found in OAS will be ignored");
                return null;
            }

            // TODO: Should PR this to swagger models
            switch (method) {
                case GET:
                    operation = pathItem.getGet();
                    break;
                case POST:
                    operation = pathItem.getPost();
                    break;
                case PUT:
                    operation = pathItem.getPut();
                    break;
                case PATCH:
                    operation = pathItem.getPatch();
                    break;
                case DELETE:
                    operation = pathItem.getDelete();
                    break;
                case HEAD:
                    operation = pathItem.getHead();
                    break;
                case OPTIONS:
                    operation = pathItem.getOptions();
                    break;
                case TRACE:
                    operation = pathItem.getTrace();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + method);
            }
            if (operation == null) {
                //TODO : replace by specialized ex
                throw new RuntimeException(String.format("Method %s %s not found in OAS !", method.toString(), path));
            }
            if (operation.getParameters() != null) {
                for (Parameter parameter : operation.getParameters()) {
                    final String parameterName = parameter.getName();
                    if (parameters.containsKey(parameterName)) {
                        if (parameter.getIn().equals("path")) {
                            // TODO: avoid multiple creations of Map
                            uriComponentsBuilder.uriVariables(Map.of(parameterName, parameters.get(parameterName)));
                        }
                        if (parameter.getIn().equals("query")) {
                            uriComponentsBuilder.queryParam(parameterName, parameters.get(parameterName));
                        }
                    } else {
                        op.addParametersItem(parameter);
                    }
                }
            }
            if (op.getRequestBody() == null) {
                op.requestBody(operation.getRequestBody());
            }
            String newPathName = uriComponentsBuilder.toUriString();
            return new Control(newPathName, method, op);
        }
    }

    public static class Control {
        private final String path;
        private final PathItem.HttpMethod method;
        private final Operation operation;

        public Control(String path, PathItem.HttpMethod method, Operation operation) {
            this.path = path;
            this.method = method;
            this.operation = operation;
        }

        public String getPath() {
            return path;
        }

        public PathItem.HttpMethod getMethod() {
            return method;
        }

        public Operation getOperation() {
            return operation;
        }
    }
}
