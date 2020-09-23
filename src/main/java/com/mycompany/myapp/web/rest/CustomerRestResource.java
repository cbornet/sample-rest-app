package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.Customer;
import com.mycompany.myapp.repository.CustomerRepository;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Customer}.
 */
@RestController
@RequestMapping(path = "/api")
@Transactional
public class CustomerRestResource {
    private final Logger log = LoggerFactory.getLogger(CustomerRestResource.class);

    private static final String ENTITY_NAME = "customer";

    private final CustomerRepository customerRepository;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper mapper;

    public CustomerRestResource(CustomerRepository customerRepository, SpringTemplateEngine templateEngine, ObjectMapper mapper) {
        this.customerRepository = customerRepository;
        this.templateEngine = templateEngine;
        this.mapper = mapper;
    }

    /**
     * {@code GET  /customers} : get all the customers.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of customers in body.
     */
    @GetMapping("/customers")
    public RestResponse getAllCustomers() throws IOException {
        log.debug("REST request to get all Customers");
        final List<Customer> customers = customerRepository.findAll();
        Context context = new Context();
        context.setVariable("customers", customers);
        String content = templateEngine.process("oai/customers.json", context);
        return new RestResponse(customers, mapper.readTree(content));
    }

    /**
     * {@code GET  /customers/:id} : get the "id" customer.
     *
     * @param id the id of the customer to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the customer, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/customers/{id}")
    public RestResponse getCustomer(@PathVariable Long id) {
        log.debug("REST request to get Customer : {}", id);

        return customerRepository
            .findById(id)
            .map(
                customer -> {
                    Context context = new Context();
                    context.setVariable("customer", customer);
                    String content = templateEngine.process("oai/customer.json", context);
                    try {
                        return new RestResponse(customer, mapper.readTree(content));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            )
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
