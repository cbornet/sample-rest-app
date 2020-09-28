package com.mycompany.myapp.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.Customer;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Customer}.
 */
@RestController
@RequestMapping(path = "/api")
@Transactional
public class CustomerRestResource {
    private final CustomerResource customerResource;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper mapper;

    public CustomerRestResource(CustomerResource customerResource, SpringTemplateEngine templateEngine, ObjectMapper mapper) {
        this.customerResource = customerResource;
        this.templateEngine = templateEngine;
        this.mapper = mapper;
    }

    /**
     * {@code GET  /customers} : get all the customers.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of customers in body.
     */
    @GetMapping("/customers")
    public RestResponse<List<Customer>> getAllCustomers() throws IOException {
        final List<Customer> customers = customerResource.getAllCustomers();
        Context context = new Context();
        context.setVariable("customers", customers);
        String content = templateEngine.process("oai/customers.json", context);
        return new RestResponse<>(customers, mapper.readTree(content));
    }

    /**
     * {@code GET  /customers/:id} : get the "id" customer.
     *
     * @param id the id of the customer to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the customer, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<RestResponse<Customer>> getCustomer(@PathVariable Long id) throws JsonProcessingException {
        final ResponseEntity<Customer> response = customerResource.getCustomer(id);
        final Customer customer = response.getBody();
        Context context = new Context();
        context.setVariable("customer", customer);
        String content = templateEngine.process("oai/customer.json", context);
        return RestResponse.wrapResponse(response, mapper.readTree(content));
    }
}
