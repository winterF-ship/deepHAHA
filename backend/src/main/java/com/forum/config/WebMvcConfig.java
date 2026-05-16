package com.forum.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final ObjectMapper objectMapper;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, ObjectMapper objectMapper) {
        this.jwtInterceptor = jwtInterceptor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(
                        "/api/user/**",
                        "/api/admin/**",
                        "/api/posts",
                        "/api/posts/*",
                        "/api/posts/*/replies",
                        "/api/posts/*/replies/*",
                        "/api/posts/*/like",
                        "/api/posts/*/favorite",
                        "/api/messages/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir").replace("\\", "/") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "https://*.trycloudflare.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
                converter.setSupportedMediaTypes(Arrays.asList(
                    MediaType.APPLICATION_JSON,
                    MediaType.TEXT_PLAIN,
                    new MediaType("application", "*+json"),
                    new MediaType("text", "*")
                ));
                converters.set(i, converter);
                break;
            }
        }
    }
}
