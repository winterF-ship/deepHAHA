package com.forum.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ContentTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String contentType = req.getContentType();
        String method = req.getMethod();
        String path = req.getRequestURI();

        boolean isWrite = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
        boolean isJson = contentType != null && contentType.contains("application/json");
        boolean isMultipart = contentType != null && contentType.contains("multipart/form-data");

        if (path.startsWith("/api/") && isWrite && !isJson && !isMultipart) {
            req = new HttpServletRequestWrapper(req) {
                @Override
                public String getContentType() {
                    return "application/json";
                }
            };
        }
        chain.doFilter(req, response);
    }
}
