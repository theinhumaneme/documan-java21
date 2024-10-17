/* Copyright (C)2024 Mudumby Kalyan / @theinhumaneme  */
package com.kalyan.documan.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  @GetMapping("/")
  public ResponseEntity<?> test() {
    return ResponseEntity.status(HttpStatus.OK).body("Hello World");
  }
}
