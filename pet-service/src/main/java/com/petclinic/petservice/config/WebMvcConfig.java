package com.petclinic.petservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Pages are serialized as a stable {@code {content, page}} DTO instead of Spring's internal
 * {@code PageImpl}, whose JSON shape is not a supported contract and logs a warning.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebMvcConfig {
}
