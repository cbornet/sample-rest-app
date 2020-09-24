package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.Order;
import com.mycompany.myapp.repository.OrderRepository;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

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
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper mapper;

    public OrderRestResource(
        OrderRepository orderRepository,
        OrderResource orderResource,
        SpringTemplateEngine templateEngine,
        ObjectMapper mapper
    ) {
        this.orderRepository = orderRepository;
        this.orderResource = orderResource;
        this.templateEngine = templateEngine;
        this.mapper = mapper;
    }

    /**
     * {@code POST  /orders} : Create a new order.
     *
     * @param order the order to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new order, or with status {@code 400 (Bad Request)} if the order has already an ID.
     */
    @PostMapping("/orders")
    public RestResponse<Order> createOrder(@RequestBody Order order) throws JsonProcessingException, URISyntaxException {
        final Order createdOrder = orderResource.createOrder(order).getBody();
        Context context = new Context();
        context.setVariable("order", createdOrder);
        String content = templateEngine.process("oai/order.json", context);
        return new RestResponse<>(createdOrder, mapper.readTree(content));
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
    public RestResponse<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) throws JsonProcessingException {
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
        Context context = new Context();
        context.setVariable("order", result);
        String content = templateEngine.process("oai/order.json", context);
        return new RestResponse<>(result, mapper.readTree(content));
    }

    /**
     * {@code GET  /orders} : get all the orders.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orders in body.
     */
    @GetMapping("/orders")
    public RestResponse<List<Order>> getAllOrders(Pageable pageable) throws IOException {
        log.debug("REST request to get a page of Orders");
        final Page<Order> page = orderRepository.findAll(pageable);
        Context context = new Context();
        context.setVariable("orders", page.getContent());
        context.setVariable("totalPages", page.getTotalPages());
        context.setVariable("currentPage", page.getNumber());
        context.setVariable("resourceUrl", "/api/orders");
        context.setVariable("pageSize", pageable.getPageSize());
        context.setVariable("totalElements", page.getTotalElements());
        final String sortParam = pageable
            .getSort()
            .stream()
            .map(order -> order.getProperty() + "," + order.getDirection())
            .collect(Collectors.joining("&"));
        context.setVariable("sortQueryParam", sortParam.isEmpty() ? "" : "&sort=" + sortParam);

        String content = templateEngine.process("oai/orders.json", context);
        return new RestResponse<>(page.getContent(), mapper.readTree(content));
    }

    /**
     * {@code GET  /orders/:id} : get the "id" order.
     *
     * @param id the id of the order to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the order, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/orders/{id}")
    public RestResponse<Order> getOrder(@PathVariable Long id) throws JsonProcessingException {
        final Order order = orderResource.getOrder(id).getBody();
        Context context = new Context();
        context.setVariable("order", order);
        String content = templateEngine.process("oai/order.json", context);
        return new RestResponse<>(order, mapper.readTree(content));
    }

    /**
     * {@code DELETE  /orders/:id} : delete the "id" order.
     *
     * @param id the id of the order to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/orders/{id}")
    public RestResponse<Void> deleteOrder(@PathVariable Long id) throws JsonProcessingException {
        orderResource.deleteOrder(id);
        String content = templateEngine.process("oai/entities.json", new Context());
        return new RestResponse<>(null, mapper.readTree(content));
    }

    @GetMapping("/customers/{id}/orders")
    public RestResponse<List<Order>> getCustomerOrders(@PathVariable Long id, Pageable pageable) throws JsonProcessingException {
        log.debug("REST request to get orders of Customer : {}", id);
        final Page<Order> page = orderRepository.findAllByCustomerId(id, pageable);

        Context context = new Context();
        context.setVariable("orders", page.getContent());
        context.setVariable("totalPages", page.getTotalPages());
        context.setVariable("currentPage", page.getNumber());
        context.setVariable("resourceUrl", "/api/customers/" + id + "/orders");
        context.setVariable("totalElements", page.getTotalElements());

        String content = templateEngine.process("oai/orders.json", context);
        return new RestResponse<>(page.getContent(), mapper.readTree(content));
    }
}
