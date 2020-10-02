package com.mycompany.myapp.config;

import static springfox.documentation.builders.PathSelectors.regex;

import io.github.jhipster.config.JHipsterConstants;
import io.github.jhipster.config.JHipsterProperties;
import io.github.jhipster.config.apidoc.customizer.SpringfoxCustomizer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_API_DOCS)
public class OpenApiConfiguration {

    @Bean
    public SpringfoxCustomizer noApiFirstCustomizer() {
        return docket -> docket.select().apis(RequestHandlerSelectors.basePackage("com.mycompany.myapp.web.api").negate());
    }

    @Bean
    public Docket apiFirstDocket(JHipsterProperties jHipsterProperties) {
        JHipsterProperties.ApiDocs properties = jHipsterProperties.getApiDocs();
        Contact contact = new Contact(properties.getContactName(), properties.getContactUrl(), properties.getContactEmail());

        ApiInfo apiInfo = new ApiInfo(
            "API First " + properties.getTitle(),
            properties.getDescription(),
            properties.getVersion(),
            properties.getTermsOfServiceUrl(),
            contact,
            properties.getLicense(),
            properties.getLicenseUrl(),
            new ArrayList<>()
        );

        return new Docket(DocumentationType.OAS_30)
            .groupName("openapi")
            .host(properties.getHost())
            .protocols(new HashSet<>(Arrays.asList(properties.getProtocols())))
            .apiInfo(apiInfo)
            .useDefaultResponseMessages(properties.isUseDefaultResponseMessages())
            .forCodeGeneration(true)
            .directModelSubstitute(ByteBuffer.class, String.class)
            .genericModelSubstitutes(ResponseEntity.class)
            .ignoredParameterTypes(Pageable.class)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.mycompany.myapp.web.api"))
            .paths(regex(properties.getDefaultIncludePattern()))
            .build();
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPIV3Parser().read("templates/oai/springfox.json");
    }
}
