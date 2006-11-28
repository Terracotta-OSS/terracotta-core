/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config;

public class TestClassExpressionMatcher implements ClassExpressionMatcher {

  public boolean shouldMatch;
  
  public boolean match(String expression) {
    return shouldMatch;
  }

}
