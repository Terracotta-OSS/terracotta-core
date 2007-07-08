/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.tc.test.server.appserver.unit.TCServletFilterHolder;
import com.tctest.webapp.servletfilters.SimpleFilter;

/**
 * Applies a simple filter to the {@link SimpleDsoSessionsTest}.
 */
public final class SimpleSessionFilterTest extends SimpleDsoSessionsTest {

  public SimpleSessionFilterTest() {
    registerFilter(new TCServletFilterHolder(SimpleFilter.class, "/*", null));
  }  
}
