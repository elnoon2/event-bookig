package com.badyauniversity.eventbooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory("uploads", registry);
        exposeFrontendDirectory(registry);
    }

    private void exposeDirectory(String dirName, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName).toAbsolutePath();
        String uriString = uploadDir.toUri().toString();
        if (!uriString.endsWith("/")) {
            uriString += "/";
        }

        String handlerPattern = dirName;
        if (handlerPattern.startsWith("../")) {
            handlerPattern = handlerPattern.replace("../", "");
        }

        registry.addResourceHandler("/" + handlerPattern + "/**")
                .addResourceLocations(uriString);
    }

    private void exposeFrontendDirectory(ResourceHandlerRegistry registry) {
        // Expose the parent folder containing the HTML/JS/CSS assets
        Path frontendDir = Paths.get("..").toAbsolutePath();
        String uriString = frontendDir.toUri().toString();
        if (!uriString.endsWith("/")) {
            uriString += "/";
        }

        registry.addResourceHandler("/**")
                .addResourceLocations(uriString);
    }
}
