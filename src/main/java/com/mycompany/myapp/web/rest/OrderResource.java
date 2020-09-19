package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.Customer;
import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.repository.OrderRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Order}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class OrderResource {
    private final Logger log = LoggerFactory.getLogger(OrderResource.class);

    private static final String ENTITY_NAME = "order";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrderRepository orderRepository;

    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderResource(OrderRepository orderRepository, SpringTemplateEngine templateEngine) {
        this.orderRepository = orderRepository;
        this.templateEngine = templateEngine;
    }

    /**
     * {@code POST  /orders} : Create a new order.
     *
     * @param order the order to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new order, or with status {@code 400 (Bad Request)} if the order has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/orders")
    public RestResponse createOrder(@RequestBody Order order) throws URISyntaxException, JsonProcessingException {
        log.debug("REST request to save Order : {}", order);
        if (order.getId() != null) {
            throw new BadRequestAlertException("A new order cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Order result = orderRepository.save(order);

        Context context = new Context();
        context.setVariable("order", result);
        String content = templateEngine.process("oai/order.json", context);
        return new RestResponse(result, mapper.readTree(content));
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
    public RestResponse updateOrder(@PathVariable Long id, @RequestBody Order order) throws JsonProcessingException {
        log.debug("REST request to update Order : {}", order);
        order.setId(id);
        Order result = orderRepository.save(order);
        Context context = new Context();
        context.setVariable("order", result);
        String content = templateEngine.process("oai/order.json", context);
        return new RestResponse(result, mapper.readTree(content));
    }

    /**
     * {@code GET  /orders} : get all the orders.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orders in body.
     */
    @GetMapping("/orders")
    public RestResponse getAllOrders(Pageable pageable) throws IOException {
        log.debug("REST request to get a page of Orders");
        final Page<Order> page = orderRepository.findAll(pageable);
        Context context = new Context();
        context.setVariable("orders", page.getContent());
        context.setVariable("totalPages", page.getTotalPages());
        context.setVariable("currentPage", page.getNumber());
        context.setVariable("resourceUrl", "/orders");
        // TODO: propagate Sort

        String content = templateEngine.process("oai/orders.json", context);
        return new RestResponse(page.getContent(), mapper.readTree(content));
        //Page<Order> page = orderRepository.findAll(pageable);
        //HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        //return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /orders/:id} : get the "id" order.
     *
     * @param id the id of the order to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the order, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/orders/{id}")
    public RestResponse getOrder(@PathVariable Long id) {
        log.debug("REST request to get Order : {}", id);
        return orderRepository
            .findById(id)
            .map(
                order -> {
                    Context context = new Context();
                    context.setVariable("order", order);
                    String content = templateEngine.process("oai/order.json", context);
                    try {
                        return new RestResponse(order, mapper.readTree(content));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            )
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        //Optional<Order> order = orderRepository.findById(id);
        //return ResponseUtil.wrapOrNotFound(order);
    }

    /**
     * {@code DELETE  /orders/:id} : delete the "id" order.
     *
     * @param id the id of the order to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/orders/{id}")
    public RestResponse deleteOrder(@PathVariable Long id) throws JsonProcessingException {
        log.debug("REST request to delete Order : {}", id);
        orderRepository.deleteById(id);

        String content = templateEngine.process("oai/entities.json", new Context());
        return new RestResponse(null, mapper.readTree(content));
    }

    @GetMapping("/customers/{id}/orders")
    public RestResponse getCustomerOrders(@PathVariable Long id, Pageable pageable) throws JsonProcessingException {
        log.debug("REST request to get orders of Customer : {}", id);
        final Page<Order> page = orderRepository.findAllByCustomerId(id, pageable);

        Context context = new Context();
        context.setVariable("orders", page.getContent());
        context.setVariable("totalPages", page.getTotalPages());
        context.setVariable("currentPage", page.getNumber());
        context.setVariable("resourceUrl", "/customers/" + id + "/orders");

        String content = templateEngine.process("oai/orders.json", context);
        return new RestResponse(page.getContent(), mapper.readTree(content));
    }
}