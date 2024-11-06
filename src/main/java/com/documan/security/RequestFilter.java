// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.info("{} - {}", request.getMethod(), request.getRequestURL());

    Enumeration<String> headerNames = request.getHeaderNames();
    StringBuilder headers = new StringBuilder();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      String headerValue = request.getHeader(headerName);
      headers.append(headerName).append(": ").append(headerValue).append(" ");
    }
    log.info("Request Headers: {}", headers);
    Enumeration<String> parameterNames = request.getParameterNames();
    StringBuilder params = new StringBuilder();
    while (parameterNames.hasMoreElements()) {
      String paramName = parameterNames.nextElement();
      String[] paramValues = request.getParameterValues(paramName);
      for (String paramValue : paramValues) {
        params.append(paramName).append(": ").append(paramValue).append(" ");
      }
    }
    log.info("Request Parameters: {}", params);

    filterChain.doFilter(request, response);
  }
}
