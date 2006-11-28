/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.unit;

import java.util.Map;

import javax.servlet.Filter;

/**
 * Implementers must provide a zero argument constructor
 */
public interface TCServletFilter extends Filter {
  
  String getPattern();
  
  Map getInitParams();
}
