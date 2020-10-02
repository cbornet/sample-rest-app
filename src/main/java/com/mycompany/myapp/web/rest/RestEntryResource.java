package com.mycompany.myapp.web.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestEntryResource {

    @GetMapping("/api")
    public OhmResponse<Void> getEntities() {
        Info info = new Info()
            .title("Demo REST application using OHM format")
            .description(
                "This is the entry point of the application. In the application, customers have orders " +
                "associated.<br>New orders can be created up to 200 orders in total. Orders with id <= 100 can't be " +
                "removed.<br>Order collections are paginated."
            );

        OpenAPI openAPI = new OpenAPI()
            .info(info)
            .path("/api/customers", new PathItem().get(new Operation().summary("Get customers")))
            .path("/api/orders", new PathItem().get(new Operation().summary("Get orders")));
        return new OhmResponse<>(null, openAPI);
    }
}
