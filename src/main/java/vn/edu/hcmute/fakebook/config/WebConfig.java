package vn.edu.hcmute.fakebook.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadLocation;

    public WebConfig(
            @Value("${app.upload-dir:uploads}")
            String uploadDirectory) {

        Path uploadPath = Paths
            .get(uploadDirectory)
            .toAbsolutePath()
            .normalize();

        String location = uploadPath
            .toUri()
            .toString();

        if (!location.endsWith("/")) {
            location += "/";
        }

        this.uploadLocation = location;
    }

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations(uploadLocation);
    }
}