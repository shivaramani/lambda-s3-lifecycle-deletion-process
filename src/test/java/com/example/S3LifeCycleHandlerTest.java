package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

class S3LifeCycleHandlerTest {
  private static final Logger logger = LoggerFactory.getLogger(S3LifeCycleHandlerTest.class);

  @Test
  void invokeTest() {
    logger.info("Invoke TEST");
    Context context = null;
    
    assertTrue(true);
  }

}