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
    private OpenAPI controls;

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

    public static <T> ResponseEntity<OhmResponse<T>> wrapResponse(ResponseEntity<T> response, OpenAPI controls) {
        final OhmResponse<T> restResponse = new OhmResponse<>(response.getBody(), controls);

        return ResponseEntity
            .status(response.getStatusCode() != HttpStatus.NO_CONTENT ? response.getStatusCode() : HttpStatus.OK)
            .headers(response.getHeaders())
            .body(restResponse);
    }

    public static Operation addControl(OpenAPI controls, OpenAPI openAPI, String pathName, PathItem.HttpMethod method) {
        return addControl(controls, openAPI, pathName, method, new HashMap<>());
    }

    public static Operation addControl(
        OpenAPI controls,
        OpenAPI openAPI,
        String pathName,
        PathItem.HttpMethod method,
        Map<String, Object> paramValues
    ) {
        return addControl(controls, openAPI, pathName, method, paramValues, null);
    }

    public static Operation addControl(
        OpenAPI controls,
        OpenAPI openAPI,
        String pathName,
        PathItem.HttpMethod method,
        Map<String, Object> paramValues,
        RequestBody requestBody
    ) {
        final PathItem pathItem = openAPI.getPaths().get(pathName);
        final Operation operation;
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(pathName);

        if (pathItem == null) {
            log.warn("Path " + pathName + "not found in OAS will be ignored");
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
            throw new RuntimeException(String.format("Method %s %s not found in OAS !", method.toString(), pathName));
        }
        Operation op = new Operation();
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                final String parameterName = parameter.getName();
                if (paramValues.containsKey(parameterName)) {
                    if (parameter.getIn().equals("path")) {
                        // TODO: avoid mutiple creations of Map
                        uriComponentsBuilder.uriVariables(Map.of(parameterName, paramValues.get(parameterName)));
                    }
                    if (parameter.getIn().equals("query")) {
                        uriComponentsBuilder.queryParam(parameterName, paramValues.get(parameterName));
                    }
                } else {
                    op.addParametersItem(parameter);
                }
            }
        }
        op.requestBody(requestBody != null ? requestBody : operation.getRequestBody());

        if (controls.getPaths() == null) {
            controls.paths(new Paths());
        }
        final Paths paths = controls.getPaths();
        String newPathName = uriComponentsBuilder.toUriString();
        paths.putIfAbsent(newPathName, new PathItem());
        final PathItem newPathItem = paths.get(newPathName);
        newPathItem.operation(method, op);
        return op;
    }

    public static void addPaginationControls(OpenAPI controls, String pathName, Operation operation, Page<?> page) {
        Operation op = new Operation();
        operation
            .getParameters()
            .stream()
            .filter(parameter -> !Set.of("page", "size", "sort").contains(parameter.getName()))
            .forEach(op::addParametersItem);
        if (controls.getPaths() == null) {
            controls.paths(new Paths());
        }
        final Paths paths = controls.getPaths();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
            .fromUriString(pathName)
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
                            .summary(String.format("%s [First page (1/%s)]", operation.getSummary(), page.getTotalPages()))
                    )
            );
        }
        if (page.getNumber() > 0) {
            paths.put(
                uriComponentsBuilder.replaceQueryParam("page", page.getNumber() - 1).toUriString(),
                new PathItem()
                .get(
                        op.summary(
                            String.format("%s [Previous page (%s/%s)]", operation.getSummary(), page.getNumber(), page.getTotalPages())
                        )
                    )
            );
        }
        if (page.getNumber() < page.getTotalPages() - 1) {
            paths.put(
                uriComponentsBuilder.replaceQueryParam("page", page.getNumber() + 1).toUriString(),
                new PathItem()
                .get(
                        new Operation()
                            .parameters(op.getParameters())
                            .summary(
                                String.format("%s [Next page (%s/%s)]", operation.getSummary(), page.getNumber() + 2, page.getTotalPages())
                            )
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
                            .summary(
                                String.format("%s [Last page (%s/%s)]", operation.getSummary(), page.getTotalPages(), page.getTotalPages())
                            )
                    )
            );
        }
    }
}
