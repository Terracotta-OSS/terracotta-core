/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
