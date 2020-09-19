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
@RequestMapping("/api")
@Transactional
public class CustomerResource {
    private final Logger log = LoggerFactory.getLogger(CustomerResource.class);

    private static final String ENTITY_NAME = "customer";

    private final CustomerRepository customerRepository;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper mapper;

    public CustomerResource(CustomerRepository customerRepository, SpringTemplateEngine templateEngine, ObjectMapper mapper) {
        this.customerRepository = customerRepository;
        this.templateEngine = templateEngine;
        this.mapper = mapper;
    }

    /**
     * {@code POST  /customers} : Create a new customer.
     *
     * @param customer the customer to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new customer, or with status {@code 400 (Bad Request)} if the customer has already an ID.
     */
    @PostMapping("/customers")
    public RestResponse createCustomer(@RequestBody Customer customer) throws IOException {
        log.debug("REST request to save Customer : {}", customer);
        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Customer result = customerRepository.save(customer);

        Context context = new Context();
        context.setVariable("customer", result);
        String content = templateEngine.process("oai/customer.json", context);
        return new RestResponse(result, mapper.readTree(content));
    }

    /**
     * {@code PUT  /customers} : Updates an existing customer.
     *
     * @param customer the customer to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated customer,
     * or with status {@code 400 (Bad Request)} if the customer is not valid,
     * or with status {@code 500 (Internal Server Error)} if the customer couldn't be updated.
     */
    @PutMapping("/customers/{id}")
    public RestResponse updateCustomer(@PathVariable Long id, @RequestBody Customer customer) throws JsonProcessingException {
        log.debug("REST request to update Customer : {}", customer);
        customer.setId(id);
        Customer result = customerRepository.save(customer);
        Context context = new Context();
        context.setVariable("customer", result);
        String content = templateEngine.process("oai/customer.json", context);
        return new RestResponse(result, mapper.readTree(content));
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

    /**
     * {@code DELETE  /customers/:id} : delete the "id" customer.
     *
     * @param id the id of the customer to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/customers/{id}")
    public RestResponse deleteCustomer(@PathVariable Long id) throws JsonProcessingException {
        log.debug("REST request to delete Customer : {}", id);
        customerRepository.deleteById(id);

        String content = templateEngine.process("oai/entities.json", new Context());
        return new RestResponse(null, mapper.readTree(content));
    }
}
