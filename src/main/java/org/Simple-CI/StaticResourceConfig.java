package org.Simple-CI;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * This is a spring configuration class responsible for handling MVC static resources
 * For this project we have used spring MVC to implement a webserver
 * This class specifies where to access CSS and JS files.
 */
@Configuration
@EnableWebMvc
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(
                "/css/**",
                "/js/**")
                .addResourceLocations(
                        "classpath:/templates/css/",
                        "classpath:/templates/js/");
    }

}
