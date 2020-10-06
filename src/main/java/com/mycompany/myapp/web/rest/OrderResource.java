package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.web.rest.OhmResponse.control;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;

import com.github.javafaker.Faker;
import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.repository.OrderRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import springfox.documentation.oas.mappers.ServiceModelToOpenApiMapper;
import springfox.documentation.service.Documentation;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Order}.
 */
@RestController
@RequestMapping(path = "/api", produces = { "application/ohm+json", "application/json" })
@Transactional
public class OrderResource {
    private final Logger log = LoggerFactory.getLogger(OrderResource.class);

    private static final String ENTITY_NAME = "order";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderRepository orderRepository;
    private final DocumentationCache documentationCache;
    private final ServiceModelToOpenApiMapper mapper;

    private OpenAPI openAPI;

    public OrderResource(OrderRepository orderRepository, DocumentationCache documentationCache, ServiceModelToOpenApiMapper mapper) {
        this.orderRepository = orderRepository;
        this.documentationCache = documentationCache;
        this.mapper = mapper;
    }

    /**
     * {@code POST  /orders} : Create a new order.
     *
     * @param order the order to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new order, or with status {@code 400 (Bad Request)} if the order has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/orders")
    public ResponseEntity<OhmResponse<Order>> createOrder(@RequestBody Order order) throws URISyntaxException {
        log.debug("REST request to save Order : {}", order);
        if (order.getId() != null) {
            throw new BadRequestAlertException("A new order cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (orderRepository.count() >= 200) {
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "Can't have more than 200 orders");
        }
        order.setProduct(new Faker().commerce().productName());
        Order result = orderRepository.save(order);
        OhmResponse<Order> ohmResponse = getOrderResponse(result);
        return ResponseEntity
            .created(new URI("/api/orders/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(ohmResponse);
    }

    /**
     * {@code PUT  /orders} : Updates an existing order.
     *
     * @param order the order to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated order,
     * or with status {@code 400 (Bad Request)} if the order is not valid,
     * or with status {@code 500 (Internal Server Error)} if the order couldn't be updated.
     */
    @Secured(AuthoritiesConstants.ADMIN)
    @PutMapping(path = "/orders", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> updateOrder(@RequestBody Order order) {
        log.debug("REST request to update Order : {}", order);
        if (order.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Order result = orderRepository.save(order);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, order.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /orders/:id} : Updates an existing order.
     *
     * @param id    the id of the order to update.
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
        return getOrderResponse(result);
    }

    /**
     * {@code GET  /orders} : get all the orders.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orders in body.
     */
    @GetMapping(value = "/orders")
    public ResponseEntity<OhmResponse<List<Order>>> getAllOrders(Pageable pageable) {
        log.debug("REST request to get a page of Orders");
        Page<Order> page = orderRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        OhmResponse<List<Order>> ohmResponse = getOrdersResponse(page, true);
        return ResponseEntity.ok().headers(headers).body(ohmResponse);
    }

    /**
     * {@code GET  /orders/:id} : get the "id" order.
     *
     * @param id the id of the order to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the order, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/orders/{id}")
    public OhmResponse<Order> getOrder(@PathVariable Long id) {
        log.debug("REST request to get Order : {}", id);
        return orderRepository
            .findById(id)
            .map(this::getOrderResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * {@code DELETE  /orders/:id} : delete the "id" order.
     *
     * @param id the id of the order to delete.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<OhmResponse<Void>> deleteOrder(@PathVariable Long id) {
        log.debug("REST request to delete Order : {}", id);
        if (id <= 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't delete orders with id <= 100");
        }
        orderRepository.deleteById(id);
        getSpringfoxOpenAPI();
        OhmResponse<Void> ohmResponse = OhmResponse
            .noContent()
            .addControl(control(openAPI, GET, "/api/customers").summary("Get all customers"))
            .addControl(control(openAPI, GET, "/api/orders").summary("Get all orders"));
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(ohmResponse);
    }

    @GetMapping("/customers/{id}/orders")
    public OhmResponse<List<Order>> getCustomerOrders(@PathVariable Long id, Pageable pageable) {
        log.debug("REST request to get orders of Customer : {}", id);
        final Page<Order> page = orderRepository.findAllByCustomerId(id, pageable);
        return getOrdersResponse(page, false);
    }

    private OhmResponse<Order> getOrderResponse(Order order) {
        getSpringfoxOpenAPI();
        OhmResponse<Order> ohmResponse = OhmResponse.of(order);

        var orderSchema = new ObjectSchema()
            .addProperties("cost", new NumberSchema().example(order.getCost()))
            .addProperties(
                "customer",
                new ObjectSchema()
                .addProperties("id", new IntegerSchema().example(order.getCustomer() != null ? order.getCustomer().getId() : null))
            );

        var requestBody = new io.swagger.v3.oas.models.parameters.RequestBody()
        .content(new Content().addMediaType("application/json", new MediaType().schema(orderSchema)));

        ohmResponse.addControl(
            control(openAPI, PUT, "/api/orders/{id}")
                .parameter("id", order.getId())
                .requestBody(requestBody)
                .summary(String.format("Update order %d", order.getId()))
        );
        if (order.getId() > 100) {
            ohmResponse.addControl(
                control(openAPI, DELETE, "/api/orders/{id}")
                    .parameter("id", order.getId())
                    .summary(String.format("Delete order %d", order.getId()))
            );
        }
        if (order.getCustomer() != null) {
            ohmResponse.addControl(
                control(openAPI, GET, "/api/customers/{id}")
                    .parameter("id", order.getCustomer().getId())
                    .summary(String.format("Get order %d customer (%d)", order.getId(), order.getCustomer().getId()))
            );
        }
        return ohmResponse.addControl(control(openAPI, GET, "/api/orders").summary("Get all orders"));
    }

    private OhmResponse<List<Order>> getOrdersResponse(Page<Order> page, boolean showCreateControl) {
        getSpringfoxOpenAPI();
        OhmResponse<List<Order>> ohmResponse = OhmResponse
            .of(page.getContent())
            .addControl(control(openAPI, GET, "/api").summary("Go to home"))
            .addPaginationControls(control(openAPI, GET, "/api/orders").summary("Get orders"), page);

        page
            .get()
            .forEach(
                order ->
                    ohmResponse.addControl(
                        control(openAPI, GET, "/api/orders/{id}")
                            .parameter("id", order.getId())
                            .summary(String.format("Get order %d", order.getId()))
                    )
            );

        if (showCreateControl) {
            if (page.getTotalElements() < 200) {
                var orderSchema = new ObjectSchema()
                    .addProperties("cost", new NumberSchema())
                    .addProperties("customer", new ObjectSchema().addProperties("id", new IntegerSchema()));
                var requestBody = new io.swagger.v3.oas.models.parameters.RequestBody()
                .content(new Content().addMediaType("application/ohm+json", new MediaType().schema(orderSchema)));
                ohmResponse.addControl(control(openAPI, POST, "/api/orders").requestBody(requestBody).summary("Create order"));
            }
        }
        return ohmResponse;
    }

    private void getSpringfoxOpenAPI() {
        if (openAPI == null) {
            Documentation documentation = documentationCache.documentationByGroup(Docket.DEFAULT_GROUP_NAME);
            openAPI = mapper.mapDocumentation(documentation);
        }
    }
}
