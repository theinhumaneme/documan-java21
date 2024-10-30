// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.kalyan.documan.security;

import com.kalyan.documan.controllers.UserController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // !TODO ensure to print all the paramters and headers along with request body and response body
    logger.info("Request URL: " + request.getRequestURL());
    logger.info("Request Method: " + request.getMethod());
    logger.info("Request Headers: " + request.getHeaderNames().toString());
    logger.info("Request Parameters: " + request.getParameterMap().toString());

    filterChain.doFilter(request, response);
  }
}
