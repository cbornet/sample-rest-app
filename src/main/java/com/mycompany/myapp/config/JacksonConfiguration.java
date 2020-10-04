package com.mycompany.myapp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycompany.myapp.web.rest.OhmResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

@Configuration
public class JacksonConfiguration {

    /**
     * Support for Java date and time API.
     * @return the corresponding Jackson module.
     */
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    public Jdk8Module jdk8TimeModule() {
        return new Jdk8Module();
    }

    /*
     * Support for Hibernate types in Jackson.
     */
    @Bean
    public Hibernate5Module hibernate5Module() {
        return new Hibernate5Module();
    }

    /*
     * Module for serialization/deserialization of RFC7807 Problem.
     */
    @Bean
    public ProblemModule problemModule() {
        return new ProblemModule();
    }

    /*
     * Module for serialization/deserialization of ConstraintViolationProblem.
     */
    @Bean
    public ConstraintViolationProblemModule constraintViolationProblemModule() {
        return new ConstraintViolationProblemModule();
    }

    @Bean
    public AbstractJackson2HttpMessageConverter ohmMessageConverter(ObjectMapper mapper) {
        ObjectMapper mapper2 = mapper.copy();
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(OhmResponse.class, new OhmResponseSerializer());
        mapper.registerModule(simpleModule);
        return new MappingJackson2OhmHttpMessageConverter(mapper2);
    }

    public static class OhmResponseSerializer extends StdSerializer<OhmResponse> {

        public OhmResponseSerializer() {
            this(null);
        }

        public OhmResponseSerializer(Class<OhmResponse> t) {
            super(t);
        }

        @Override
        public void serialize(OhmResponse value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.getContent());
        }
    }

    public static class MappingJackson2OhmHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

        public MappingJackson2OhmHttpMessageConverter() {
            this(Jackson2ObjectMapperBuilder.json().build());
        }

        public MappingJackson2OhmHttpMessageConverter(ObjectMapper objectMapper) {
            super(objectMapper, MediaType.valueOf("application/ohm+json"));
        }
    }
}
