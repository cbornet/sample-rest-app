package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.web.rest.OhmResponse.addControl;
import static com.mycompany.myapp.web.rest.OhmResponse.addPaginationControls;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;

import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.repository.OrderRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Order}.
 */
@RestController
@RequestMapping(path = "/api")
@Transactional
public class OrderRestResource {
    private final Logger log = LoggerFactory.getLogger(OrderRestResource.class);

    private final OrderRepository orderRepository;
    private final OrderResource orderResource;
    private final OpenAPI openAPI;

    public OrderRestResource(OrderRepository orderRepository, OrderResource orderResource, OpenAPI openAPI) {
        this.orderRepository = orderRepository;
        this.orderResource = orderResource;
        this.openAPI = openAPI;
    }

    /**
     * {@code POST  /orders} : Create a new order.
     *
     * @param order the order to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new order, or with status {@code 400 (Bad Request)} if the order has already an ID.
     */
    @PostMapping("/orders")
    public ResponseEntity<OhmResponse<Order>> createOrder(@RequestBody Order order) throws URISyntaxException {
        final ResponseEntity<Order> response = orderResource.createOrder(order);
        return OhmResponse.wrapResponse(response, getOrderControls(order));
    }

    /**
     * {@code PUT  /orders} : Updates an existing order.
     *
     * @param order the order to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated order,
     * or with status {@code 400 (Bad Request)} if the order is not valid,
     * or with status {@code 500 (Internal Server Error)} if the order couldn't be updated.
     */
    @PutMapping("/orders/{id}")
    public OhmResponse<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        log.debug("REST request to update Order : {}", order);
        order.setId(id);
        orderRepository
            .findById(id)
            .ifPresentOrElse(
                existingOrder -> order.setProduct(existingOrder.getProduct()),
                () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
            );
        Order result = orderRepository.save(order);
        return new OhmResponse<>(result, getOrderControls(order));
    }

    /**
     * {@code GET  /orders} : get all the orders.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orders in body.
     */
    @GetMapping("/orders")
    public OhmResponse<List<Order>> getAllOrders(Pageable pageable) {
        log.debug("REST request to get a page of Orders");
        final Page<Order> page = orderRepository.findAll(pageable);
        OpenAPI controls = getOrdersControls(page, true);

        return new OhmResponse<>(page.getContent(), controls);
    }

    /**
     * {@code GET  /orders/:id} : get the "id" order.
     *
     * @param id the id of the order to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the order, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OhmResponse<Order>> getOrder(@PathVariable Long id) {
        final ResponseEntity<Order> response = orderResource.getOrder(id);
        final Order order = response.getBody();
        return OhmResponse.wrapResponse(response, getOrderControls(order));
    }

    /**
     * {@code DELETE  /orders/:id} : delete the "id" order.
     *
     * @param id the id of the order to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<OhmResponse<Void>> deleteOrder(@PathVariable Long id) {
        final ResponseEntity<Void> response = orderResource.deleteOrder(id);
        OpenAPI controls = new OpenAPI();
        addControl(controls, openAPI, "/api/customers", GET).summary("Get all customers");
        addControl(controls, openAPI, "/api/orders", GET).summary("Get all orders");
        return OhmResponse.wrapResponse(response, controls);
    }

    @GetMapping("/customers/{id}/orders")
    public OhmResponse<List<Order>> getCustomerOrders(@PathVariable Long id, Pageable pageable) {
        log.debug("REST request to get orders of Customer : {}", id);
        final Page<Order> page = orderRepository.findAllByCustomerId(id, pageable);

        final OpenAPI controls = getOrdersControls(page, false);
        return new OhmResponse<>(page.getContent(), controls);
    }

    private OpenAPI getOrderControls(Order order) {
        var controls = new OpenAPI();

        var orderSchema = new ObjectSchema()
            .addProperties("cost", new NumberSchema().example(order.getCost()))
            .addProperties(
                "customer",
                new ObjectSchema()
                .addProperties("id", new IntegerSchema().example(order.getCustomer() != null ? order.getCustomer().getId() : null))
            );
        var requestBody = new io.swagger.v3.oas.models.parameters.RequestBody()
        .content(new Content().addMediaType("application/ohm+json", new MediaType().schema(orderSchema)));
        addControl(controls, openAPI, "/api/orders/{id}", PUT, Map.of("id", order.getId()), requestBody)
            .summary(String.format("Delete order %d", order.getId()));
        if (order.getId() > 100) {
            addControl(controls, openAPI, "/api/orders/{id}", DELETE, Map.of("id", order.getId()))
                .summary(String.format("Delete order %d", order.getId()));
        }
        if (order.getCustomer() != null) {
            addControl(controls, openAPI, "/api/customers/{id}", GET, Map.of("id", order.getCustomer().getId()))
                .summary(String.format("Get order %d customer (%d)", order.getId(), order.getCustomer().getId()));
        }
        addControl(controls, openAPI, "/api/orders", GET).summary("Get all orders");
        return controls;
    }

    private OpenAPI getOrdersControls(Page<Order> page, boolean showCreateControl) {
        OpenAPI controls = new OpenAPI();

        addControl(controls, openAPI, "/api", GET).summary("Go to home");

        final Operation operation = addControl(controls, openAPI, "/api/orders", GET).summary("Get all orders");

        addPaginationControls(controls, "/api/orders", operation, page);

        page
            .get()
            .forEach(
                order ->
                    addControl(controls, openAPI, "/api/orders/{id}", GET, Map.of("id", order.getId()))
                        .summary(String.format("Get order %d", order.getId()))
            );
        addControl(controls, openAPI, "/api/orders", GET).summary("Get all orders");
        if (showCreateControl) {
            if (page.getTotalElements() < 200) {
                var orderSchema = new ObjectSchema()
                    .addProperties("cost", new NumberSchema())
                    .addProperties("customer", new ObjectSchema().addProperties("id", new IntegerSchema()));
                var requestBody = new io.swagger.v3.oas.models.parameters.RequestBody()
                .content(new Content().addMediaType("application/ohm+json", new MediaType().schema(orderSchema)));
                addControl(controls, openAPI, "/api/orders", POST, new HashMap<>(), requestBody).summary("Create order");
            }
        }
        return controls;
    }
}
