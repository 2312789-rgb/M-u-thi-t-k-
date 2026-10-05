package vn.macstudio.macstudio.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private final String location;
    public UploadResourceConfig(@Value("${store.upload-dir:uploads}") String uploadDirectory) {
        this.location = Path.of(uploadDirectory).toAbsolutePath().normalize().toUri().toString();
    }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}